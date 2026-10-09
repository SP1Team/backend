package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.*;
import cz.fit.cvut.seatlock.domain.enums.TicketStatus;
import cz.fit.cvut.seatlock.domain.enums.TicketType;
import cz.fit.cvut.seatlock.dto.*;
import cz.fit.cvut.seatlock.es_documents.ActivityDocument;
import cz.fit.cvut.seatlock.es_documents.ActivitySearchRepository;
import cz.fit.cvut.seatlock.events.ActivityEventDTO;
import cz.fit.cvut.seatlock.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final ActivityCategoryRepository activityCategoryRepository;
    private final HallRepository hallRepository;
    private final PerformerRepository performerRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final OfferRepository offerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ActivitySearchRepository activitySearchRepository;

    public Boolean createActivity(ActivityDTO dto) {
        if (activityRepository.findByName(dto.name()).isPresent()) {
            throw new RuntimeException("Activity with this name already exists");
        }

        Hall hall = hallRepository.findById(dto.hallId()).orElseThrow(() -> new RuntimeException("Hall not found"));

        ActivityCategory activityCategory = activityCategoryRepository.findById(dto.categoryId()).orElseThrow(() -> new RuntimeException("Activity category not found"));

        if (!activityRepository.availableTimeForActivity(dto.hallId(), dto.date(), dto.startTime(), dto.endTime(), null)) {
            throw new RuntimeException("Hall is not available at this time");
        }

        List<Performer> performers = findPerformersOrThrow(dto.performer());
        Map<UUID, Seat> priceZoneSeats = findPriceZoneSeatsOrThrow(dto.priceZones());

        Activity newActivity = new Activity(dto.name(), dto.date(), dto.startTime(), dto.endTime(), dto.description(), activityCategory, hall, performers);
        List<Offer> offers = new ArrayList<>();
        if (dto.priceZones() != null && !dto.priceZones().isEmpty()) {
            for (PriceZoneDTO zone : dto.priceZones()) {
                Offer offer = new Offer();
                offer.setActivity(newActivity);
                offer.setPrice(zone.price());
                offer.setType(zone.ticketType());

                if (zone.seatIds() != null && !zone.seatIds().isEmpty()) {
                    List<Seat> zoneSeats = zone.seatIds().stream()
                            .map(priceZoneSeats::get)
                            .toList();
                    offer.setSeats(zoneSeats);
                }
                offers.add(offer);
            }
        }
        OfferSeatIndex.create(offers);

        Activity saved = activityRepository.save(newActivity);
        offers.forEach(offer -> offer.setActivity(saved));
        offers.forEach(offerRepository::save);

        if (!performers.isEmpty()) {
            activityRepository.savePerformers(saved.getId(), performers);
        }

        eventPublisher.publishEvent(new ActivityEventDTO(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                performers.stream().map(Performer::getName).toList()
        ));

        return true;
    }

    private List<Performer> findPerformersOrThrow(List<UUID> performerIds) {
        if (performerIds == null || performerIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Performer> performers = performerRepository.findAllByIds(performerIds);
        ensureAllIdsFound(
                performerIds,
                performers.stream().map(Performer::getId).collect(Collectors.toSet()),
                "Performer"
        );
        return performers;
    }

    private Map<UUID, Seat> findPriceZoneSeatsOrThrow(List<PriceZoneDTO> priceZones) {
        if (priceZones == null || priceZones.isEmpty()) {
            return Map.of();
        }

        List<UUID> seatIds = priceZones.stream()
                .filter(zone -> zone.seatIds() != null)
                .flatMap(zone -> zone.seatIds().stream())
                .toList();
        if (seatIds.isEmpty()) {
            return Map.of();
        }

        List<Seat> seats = seatRepository.findAllByIds(seatIds);
        Map<UUID, Seat> seatsById = seats.stream()
                .collect(Collectors.toMap(Seat::getId, seat -> seat));
        ensureAllIdsFound(seatIds, seatsById.keySet(), "Seat");
        return seatsById;
    }

    private void ensureAllIdsFound(List<UUID> requestedIds, Set<UUID> foundIds, String entityName) {
        Set<UUID> missingIds = new LinkedHashSet<>(requestedIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException(entityName + " IDs not found: " + missingIds);
        }
    }

    public ActivityPage getAllActivities(int page, int size) {
        long total = activityRepository.count();
        List<Activity> content = activityRepository.findAll(page, size);
        int totalPages = (int) Math.ceil((double) total / size);
        return new ActivityPage(content, total, totalPages, page);
    }

    public Activity getActivityById(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    public Boolean deleteActivity(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        activityRepository.deletePerformers(id);
        activityRepository.delete(activity);
        activitySearchRepository.deleteById(id.toString());
        return true;
    }

    public Boolean updateActivity(ActivityDTO dto, UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        if (dto.name() != null) {
            activityRepository.findByName(dto.name()).ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new RuntimeException("Activity with this name already exists");
                }
            });
        }

        UUID hallId = dto.hallId() != null ? dto.hallId() : activity.getHallId();
        LocalDate date = dto.date() != null ? dto.date() : activity.getDate();
        LocalTime startTime = dto.startTime() != null ? dto.startTime() : activity.getStartTime();
        LocalTime endTime = dto.endTime() != null ? dto.endTime() : activity.getEndTime();

        if (!activityRepository.availableTimeForActivity(hallId, date, startTime, endTime, id)) {
            throw new RuntimeException("Hall is not available at this time");
        }

        if (dto.name() != null) activity.setName(dto.name());
        if (dto.date() != null) activity.setDate(dto.date());
        if (dto.startTime() != null) activity.setStartTime(dto.startTime());
        if (dto.endTime() != null) activity.setEndTime(dto.endTime());
        if (dto.description() != null) activity.setDescription(dto.description());

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new RuntimeException("Hall not found"));
        activity.setHall(hall);

        UUID categoryId = dto.categoryId() != null ? dto.categoryId() : activity.getActivityCategoryId();
        ActivityCategory category = activityCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        activity.setCategory(category);

        List<Performer> performers;
        if (dto.performer() != null) {
            performers = findPerformersOrThrow(dto.performer());
            activityRepository.deletePerformers(id);
            if (!performers.isEmpty()) activityRepository.savePerformers(id, performers);
        } else {
            performers = performerRepository.findAllByIds(
                    activityRepository.findPerformerIdsByActivityId(id)
            );
        }

        activityRepository.save(activity);

        eventPublisher.publishEvent(new ActivityEventDTO(
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                performers.stream().map(Performer::getName).toList()
        ));
        return true;
    }

    public SeatMapResponse getSeatMap(UUID activityId) {
        Activity activity = activityRepository.findById(activityId).orElseThrow(() -> new RuntimeException("Activity not found"));

        List<Seat> seats = seatRepository.findByHallId(activity.getHallId());
        List<Offer> offers = offerRepository.findByActivityId(activityId);

        List<PriceZoneInfo> priceZones = offers.stream()
                .map(o -> new PriceZoneInfo(o.getType(), o.getPrice()))
                .toList();

        Map<UUID, Offer> offerBySeatId = OfferSeatIndex.create(offers);

        Map<Integer, List<SeatMapTicket>> ticketsByRow = new LinkedHashMap<>();
        for (Seat seat : seats) {
            Offer offer = offerBySeatId.get(seat.getId());
            TicketType type = offer != null ? offer.getType() : TicketType.STANDARD;

            SeatMapTicket ticket = new SeatMapTicket(seat.getId(), seat.getSeatNumber(), seat.getRowNumber(), type);
            ticketsByRow.computeIfAbsent(seat.getRowNumber(), k -> new ArrayList<>()).add(ticket);
        }

        List<SeatMapRow> rows = ticketsByRow.entrySet().stream()
                .map(entry -> new SeatMapRow(entry.getKey(), entry.getValue()))
                .toList();

        return new SeatMapResponse(rows, priceZones);
    }

    public Boolean setOffers(UUID activityId, SetOffersInput input) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // типы: name -> Offer (пока без seats)
        Map<String, Offer> offerByTypeName = new HashMap<>();
        for (TicketTypeInput t : input.types()) {
            String typeName = t.name().toUpperCase(Locale.ROOT);
            if (offerByTypeName.containsKey(typeName)) {
                throw new IllegalArgumentException("Duplicate ticket type: " + typeName);
            }

            TicketType ticketType = TicketType.valueOf(typeName);
            Offer offer = new Offer();
            offer.setActivity(activity);
            offer.setType(ticketType);
            offer.setPrice(t.price());
            offerByTypeName.put(typeName, offer);
        }

        // загружаем все места зала, группируем по ряду
        List<Seat> allSeats = seatRepository.findByHallId(activity.getHallId());
        Map<Integer, List<Seat>> seatsByRow = new HashMap<>();
        for (Seat seat : allSeats) {
            seatsByRow.computeIfAbsent(seat.getRowNumber(), k -> new ArrayList<>()).add(seat);
        }

        // раскладываем места по офферам
        Map<String, Set<UUID>> assignedSeatIdsByType = new HashMap<>();
        for (RowOfferInput rowOffer : input.offers()) {
            String typeName = rowOffer.typeName().toUpperCase(Locale.ROOT);
            Offer offer = offerByTypeName.get(typeName);
            if (offer == null) throw new RuntimeException("Type not found: " + rowOffer.typeName());
            List<Seat> rowSeats = seatsByRow.get(rowOffer.row());
            if (rowSeats == null) {
                throw new IllegalArgumentException("Row not found in activity hall: " + rowOffer.row());
            }
            Set<UUID> assignedSeatIds = assignedSeatIdsByType.computeIfAbsent(typeName, ignored -> new HashSet<>());
            rowSeats.stream()
                    .filter(seat -> assignedSeatIds.add(seat.getId()))
                    .forEach(offer.getSeats()::add);
        }

        List<Offer> offers = new ArrayList<>(offerRepository.findByActivityId(activityId));
        offers.addAll(offerByTypeName.values());
        OfferSeatIndex.create(offers);

        offerByTypeName.values().forEach(offerRepository::save);
        return true;
    }

    public List<String> getCities() {
        return activityRepository.findDistinctCities();
    }

    public List<Activity> search(SearchInputDTO dto){
        if(dto.text() == null || dto.text().isBlank()){
            return activityRepository.findWithFilters(null, dto.date(), dto.dateFrom(), dto.dateTo(), dto.startTime(), dto.category(), dto.city());
        }
        List<ActivityDocument> docs = activitySearchRepository.searchByTextWithPerformers(dto.text());
        List<UUID> ids = docs.stream().map(doc -> UUID.fromString(doc.getId())).toList();
        if(ids.isEmpty()){
            return new ArrayList<>();
        }else{
            return activityRepository.findWithFilters(ids, dto.date(), dto.dateFrom(), dto.dateTo(), dto.startTime(), dto.category(), dto.city());
        }
    }
}
