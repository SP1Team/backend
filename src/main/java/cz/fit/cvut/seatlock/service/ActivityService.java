package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.*;
import cz.fit.cvut.seatlock.domain.enums.TicketType;
import cz.fit.cvut.seatlock.dto.*;
import cz.fit.cvut.seatlock.es_documents.ActivityDocument;
import cz.fit.cvut.seatlock.es_documents.ActivitySearchRepository;
import cz.fit.cvut.seatlock.events.ActivityEventDTO;
import cz.fit.cvut.seatlock.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
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

    @Transactional
    public Boolean createActivity(ActivityRequest request) {
        if (activityRepository.findByName(request.name()).isPresent()) {
            throw new RuntimeException("Activity with this name already exists");
        }

        Hall hall = hallRepository.findById(request.hallId()).orElseThrow(() -> new RuntimeException("Hall not found"));

        ActivityCategory activityCategory = activityCategoryRepository.findById(request.categoryId()).orElseThrow(() -> new RuntimeException("Activity category not found"));

        if (request.startTime() != null && request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (!activityRepository.availableTimeForActivity(request.hallId(), request.date(), request.startTime(), request.endTime(), null)) {
            throw new RuntimeException("Hall is not available at this time");
        }

        List<Performer> performers = findPerformersOrThrow(request.performer());
        Map<UUID, Seat> priceZoneSeats = findPriceZoneSeatsOrThrow(request.priceZones(), request.hallId());

        Activity newActivity = new Activity(request.name(), request.date(), request.startTime(), request.endTime(), request.description(), activityCategory, hall, performers);
        List<Offer> offers = new ArrayList<>();
        if (request.priceZones() != null && !request.priceZones().isEmpty()) {
            for (PriceZoneRequest zone : request.priceZones()) {
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

    private Map<UUID, Seat> findPriceZoneSeatsOrThrow(List<PriceZoneRequest> priceZones, UUID hallId) {
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

        List<Seat> seats = seatRepository.findAllByIdsAndHallId(seatIds, hallId);
        Map<UUID, Seat> seatsById = seats.stream()
                .collect(Collectors.toMap(Seat::getId, seat -> seat));
        ensureAllIdsFound(seatIds, seatsById.keySet(), "Seat in hall " + hallId);
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

    @Transactional
    public Boolean deleteActivity(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        offerRepository.deleteByActivityId(id);
        activityRepository.deletePerformers(id);
        activityRepository.delete(activity);
        try {
            activitySearchRepository.deleteById(id.toString());
        } catch (Exception e) {
            log.error("Failed to delete activity from Elasticsearch index for id: {}", id, e);
        }
        return true;
    }

    @Transactional
    public Boolean updateActivity(ActivityRequest request, UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        if (request.name() != null) {
            activityRepository.findByName(request.name()).ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new RuntimeException("Activity with this name already exists");
                }
            });
        }

        UUID hallId = request.hallId() != null ? request.hallId() : activity.getHallId();
        LocalDate date = request.date() != null ? request.date() : activity.getDate();
        LocalTime startTime = request.startTime() != null ? request.startTime() : activity.getStartTime();
        LocalTime endTime = request.endTime() != null ? request.endTime() : activity.getEndTime();

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (!activityRepository.availableTimeForActivity(hallId, date, startTime, endTime, id)) {
            throw new RuntimeException("Hall is not available at this time");
        }

        if (request.name() != null) activity.setName(request.name());
        if (request.date() != null) activity.setDate(request.date());
        if (request.startTime() != null) activity.setStartTime(request.startTime());
        if (request.endTime() != null) activity.setEndTime(request.endTime());
        if (request.description() != null) activity.setDescription(request.description());

        boolean hallChanged = request.hallId() != null && !request.hallId().equals(activity.getHallId());
        if (hallChanged) {
            var purchasedSeats = ticketRepository.findPurchasedSeatIdsByActivityId(id);
            if (purchasedSeats != null && !purchasedSeats.isEmpty()) {
                throw new RuntimeException("Cannot change hall for an activity with purchased tickets");
            }
            offerRepository.deleteByActivityId(id);
        }

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new RuntimeException("Hall not found"));
        activity.setHall(hall);

        if (request.priceZones() != null && !request.priceZones().isEmpty()) {
            if (!hallChanged) {
                var purchasedSeats = ticketRepository.findPurchasedSeatIdsByActivityId(id);
                if (purchasedSeats != null && !purchasedSeats.isEmpty()) {
                    throw new RuntimeException("Cannot update price zones for an activity with purchased tickets");
                }
                offerRepository.deleteByActivityId(id);
            }

            Map<UUID, Seat> priceZoneSeats = findPriceZoneSeatsOrThrow(request.priceZones(), hallId);
            List<Offer> newOffers = new ArrayList<>();
            for (PriceZoneRequest zone : request.priceZones()) {
                Offer offer = new Offer();
                offer.setActivity(activity);
                offer.setPrice(zone.price());
                offer.setType(zone.ticketType());

                if (zone.seatIds() != null && !zone.seatIds().isEmpty()) {
                    List<Seat> zoneSeats = zone.seatIds().stream()
                            .map(priceZoneSeats::get)
                            .toList();
                    offer.setSeats(zoneSeats);
                }
                newOffers.add(offer);
            }
            OfferSeatIndex.create(newOffers);
            newOffers.forEach(offerRepository::save);
        }

        UUID categoryId = request.categoryId() != null ? request.categoryId() : activity.getActivityCategoryId();
        ActivityCategory category = activityCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        activity.setCategory(category);

        List<Performer> performers;
        if (request.performer() != null) {
            performers = findPerformersOrThrow(request.performer());
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

    @Transactional
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

    public List<Activity> search(ActivitySearchRequest request){
        if(request.text() == null || request.text().isBlank()){
            return activityRepository.findWithFilters(null, request.date(), request.dateFrom(), request.dateTo(), request.startTime(), request.category(), request.city());
        }
        List<ActivityDocument> docs = activitySearchRepository.searchByTextWithPerformers(request.text());
        List<UUID> ids = docs.stream().map(doc -> UUID.fromString(doc.getId())).toList();
        if(ids.isEmpty()){
            return new ArrayList<>();
        }else{
            return activityRepository.findWithFilters(ids, request.date(), request.dateFrom(), request.dateTo(), request.startTime(), request.category(), request.city());
        }
    }
}
