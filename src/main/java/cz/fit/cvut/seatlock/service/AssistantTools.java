package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Activity;
import cz.fit.cvut.seatlock.domain.ActivityCategory;
import cz.fit.cvut.seatlock.domain.Hall;
import cz.fit.cvut.seatlock.domain.Order;
import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.ActivitySearchRequest;
import cz.fit.cvut.seatlock.dto.OrderResponse;
import cz.fit.cvut.seatlock.dto.PriceZoneInfo;
import cz.fit.cvut.seatlock.dto.SeatMapResponse;
import cz.fit.cvut.seatlock.dto.SeatMapRow;
import cz.fit.cvut.seatlock.dto.SeatMapTicket;
import cz.fit.cvut.seatlock.repository.ActivityCategoryRepository;
import cz.fit.cvut.seatlock.repository.HallRepository;
import cz.fit.cvut.seatlock.repository.PerformerRepository;
import cz.fit.cvut.seatlock.repository.TicketRepository;
import cz.fit.cvut.seatlock.security.SeatlockUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only tools the assistant is allowed to call. Every method returns plain text that is fed back
 * to the model, so the model never sees entities, only the facts we choose to expose.
 * <p>
 * Tools never accept a user id: personal data is resolved from the security context, otherwise the
 * model would be able to read somebody else's orders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantTools {

    private static final int MAX_ACTIVITIES = 10;
    private static final int MAX_ROWS = 25;
    private static final int MAX_ORDERS = 10;
    private static final int MAX_FAVORITES = 10;

    private static final String NOT_SIGNED_IN =
            "The user is not signed in, so personal data is not available. Ask them to sign in first.";
    private static final String SEARCH_UNAVAILABLE =
            "Search is temporarily unavailable. Suggest filtering by date, city or category instead.";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ActivityService activityService;
    private final OrderService orderService;
    private final FavoriteService favoriteService;
    private final TicketRepository ticketRepository;
    private final HallRepository hallRepository;
    private final ActivityCategoryRepository activityCategoryRepository;
    private final PerformerRepository performerRepository;

    @Tool(name = "search_activities",
            description = "Search Seatlock events (activities) by free text, date range, city and category. "
                    + "Returns up to " + MAX_ACTIVITIES + " upcoming events with id, name, date, time, city, "
                    + "hall and category. Always call this tool before talking about a concrete event.")
    public String searchActivities(
            @ToolParam(required = false,
                    description = "Free text matched against event name, description and performers, e.g. 'jazz' (optional)")
            String text,
            @ToolParam(required = false, description = "Earliest event date as yyyy-MM-dd (optional, defaults to today)")
            String dateFrom,
            @ToolParam(required = false, description = "Latest event date as yyyy-MM-dd (optional)")
            String dateTo,
            @ToolParam(required = false, description = "City name exactly as returned by list_cities (optional)")
            String city,
            @ToolParam(required = false, description = "Category name exactly as returned by list_categories (optional)")
            String category) {

        LocalDate from;
        LocalDate to;
        try {
            from = parseDate(dateFrom);
            to = parseDate(dateTo);
        } catch (DateTimeParseException e) {
            return "Invalid date format, expected yyyy-MM-dd.";
        }
        if (from == null && to == null) {
            from = LocalDate.now();
        }
        if (from != null && to != null && from.isAfter(to)) {
            return "The start date must not be after the end date.";
        }

        List<Activity> activities;
        try {
            activities = activityService.search(new ActivitySearchRequest(
                    blankToNull(text), null, from, to, null, blankToNull(category), blankToNull(city)));
        } catch (RuntimeException e) {
            log.warn("Assistant search failed", e);
            return SEARCH_UNAVAILABLE;
        }
        if (activities.isEmpty()) {
            return "No events found for these filters.";
        }

        List<Activity> sorted = activities.stream()
                .sorted(Comparator.comparing(Activity::getDate).thenComparing(Activity::getStartTime))
                .toList();
        List<Activity> shown = sorted.stream().limit(MAX_ACTIVITIES).toList();

        Map<UUID, Hall> halls = hallsById(shown);
        Map<UUID, ActivityCategory> categories = categoriesById(shown);

        StringBuilder answer = new StringBuilder("Found ").append(sorted.size()).append(" event(s)");
        if (sorted.size() > shown.size()) {
            answer.append(", showing the first ").append(shown.size());
        }
        answer.append(":\n");
        for (Activity activity : shown) {
            answer.append("- ")
                    .append(formatActivity(activity, halls.get(activity.getHallId()),
                            categories.get(activity.getActivityCategoryId())))
                    .append('\n');
        }
        return answer.toString().stripTrailing();
    }

    @Tool(name = "get_activity_details",
            description = "Full details of one event by id: description, hall address, performers, ticket types "
                    + "with prices and the number of free seats.")
    public String getActivityDetails(
            @ToolParam(description = "Event id returned by search_activities") String activityId) {

        UUID id = parseUuid(activityId);
        if (id == null) {
            return "Invalid event id.";
        }

        Activity activity;
        try {
            activity = activityService.getActivityById(id);
        } catch (RuntimeException e) {
            return "Event " + activityId + " was not found.";
        }

        Hall hall = hallRepository.findById(activity.getHallId()).orElse(null);
        ActivityCategory category = activityCategoryRepository.findById(activity.getActivityCategoryId()).orElse(null);
        List<Performer> performers = performerRepository.findAllByActivityIds(List.of(id))
                .getOrDefault(id, List.of());
        SeatOverview seats = loadSeatOverview(id);

        StringBuilder answer = new StringBuilder();
        answer.append("Event ").append(activity.getName()).append(" (id=").append(activity.getId()).append(")\n");
        answer.append("Date: ").append(activity.getDate()).append(' ')
                .append(formatTime(activity.getStartTime())).append('-')
                .append(formatTime(activity.getEndTime())).append('\n');
        answer.append("Category: ").append(category != null ? category.getName() : "unknown").append('\n');
        if (hall != null) {
            answer.append("Hall: ").append(hall.getName()).append(", ")
                    .append(hall.getAddress()).append(", ").append(hall.getCity()).append('\n');
        }
        if (activity.getDescription() != null && !activity.getDescription().isBlank()) {
            answer.append("Description: ").append(activity.getDescription()).append('\n');
        }
        if (!performers.isEmpty()) {
            answer.append("Performers: ")
                    .append(performers.stream().map(Performer::getName).collect(Collectors.joining(", ")))
                    .append('\n');
        }
        answer.append("Ticket types: ").append(formatPrices(seats.priceZones())).append('\n');
        answer.append("Free seats: ").append(seats.freeSeats()).append(" of ").append(seats.totalSeats());
        return answer.toString();
    }

    @Tool(name = "get_seat_availability",
            description = "Free seats per row for one event with ticket types and prices. Use it before "
                    + "recommending seats; it does not reserve anything.")
    public String getSeatAvailability(
            @ToolParam(description = "Event id returned by search_activities") String activityId) {

        UUID id = parseUuid(activityId);
        if (id == null) {
            return "Invalid event id.";
        }

        SeatOverview seats;
        try {
            seats = loadSeatOverview(id);
        } catch (RuntimeException e) {
            return "Event " + activityId + " was not found.";
        }
        if (seats.totalSeats() == 0) {
            return "This event has no seats configured yet.";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("Free seats: ").append(seats.freeSeats()).append(" of ").append(seats.totalSeats()).append('\n');
        answer.append("Ticket types: ").append(formatPrices(seats.priceZones())).append('\n');
        for (RowSeats row : seats.rows().stream().limit(MAX_ROWS).toList()) {
            answer.append("- row ").append(row.number()).append(": ")
                    .append(row.freeSeats()).append(" free of ").append(row.totalSeats());
            if (!row.ticketTypes().isEmpty()) {
                answer.append(" (").append(String.join(", ", row.ticketTypes())).append(')');
            }
            answer.append('\n');
        }
        if (seats.rows().size() > MAX_ROWS) {
            answer.append("... ").append(seats.rows().size() - MAX_ROWS).append(" more rows omitted.\n");
        }
        return answer.toString().stripTrailing();
    }

    @Tool(name = "list_cities", description = "Cities that currently have Seatlock events.")
    public String listCities() {
        List<String> cities = activityService.getCities();
        return cities.isEmpty() ? "No cities with events yet." : String.join(", ", cities);
    }

    @Tool(name = "list_categories", description = "Event categories available on Seatlock.")
    public String listCategories() {
        List<String> categories = activityCategoryRepository.findAll().stream()
                .map(ActivityCategory::getName)
                .sorted()
                .toList();
        return categories.isEmpty() ? "No categories yet." : String.join(", ", categories);
    }

    @Tool(name = "get_my_upcoming_orders",
            description = "Orders of the signed-in user for upcoming events, with seats, total price and status. "
                    + "Requires a signed-in user.")
    public String getMyUpcomingOrders() {
        UUID userId = currentUserId();
        if (userId == null) {
            return NOT_SIGNED_IN;
        }

        List<Order> orders = orderService.getUpcomingOrdersByUserId(userId);
        if (orders.isEmpty()) {
            return "The user has no upcoming orders.";
        }

        StringBuilder answer = new StringBuilder();
        for (Order order : orders.stream().limit(MAX_ORDERS).toList()) {
            OrderResponse detail = orderService.getOrderById(order.getId());
            String seats = detail.tickets().stream()
                    .map(ticket -> "row " + ticket.rowNumber() + " seat " + ticket.seatNumber())
                    .collect(Collectors.joining(", "));
            Hall hall = detail.activity().hall();
            answer.append("- order ").append(detail.id())
                    .append(" | ").append(detail.activity().name())
                    .append(" | ").append(detail.activity().date())
                    .append(' ').append(detail.activity().startTime())
                    .append(hall != null ? " | " + hall.getName() + ", " + hall.getCity() : "")
                    .append(" | ").append(seats)
                    .append(" | total ").append(formatPrice(detail.totalPrice()))
                    .append(" | status ").append(detail.status())
                    .append('\n');
        }
        if (orders.size() > MAX_ORDERS) {
            answer.append("... ").append(orders.size() - MAX_ORDERS).append(" more orders omitted.\n");
        }
        return answer.toString().stripTrailing();
    }

    @Tool(name = "get_my_favorite_activities",
            description = "Events the signed-in user marked as favourite. Requires a signed-in user.")
    public String getMyFavoriteActivities() {
        UUID userId = currentUserId();
        if (userId == null) {
            return NOT_SIGNED_IN;
        }

        List<Activity> favorites = favoriteService.getFavoriteActivities(userId);
        if (favorites.isEmpty()) {
            return "The user has no favourite events yet.";
        }

        List<Activity> shown = favorites.stream().limit(MAX_FAVORITES).toList();
        Map<UUID, Hall> halls = hallsById(shown);
        StringBuilder answer = new StringBuilder();
        for (Activity activity : shown) {
            answer.append("- ")
                    .append(formatActivity(activity, halls.get(activity.getHallId()), null))
                    .append('\n');
        }
        if (favorites.size() > shown.size()) {
            answer.append("... ").append(favorites.size() - shown.size()).append(" more events omitted.\n");
        }
        return answer.toString().stripTrailing();
    }

    private SeatOverview loadSeatOverview(UUID activityId) {
        SeatMapResponse seatMap = activityService.getSeatMap(activityId);
        Set<UUID> soldSeats = new HashSet<>(ticketRepository.findPurchasedSeatIdsByActivityId(activityId));

        List<RowSeats> rows = new ArrayList<>();
        int freeSeats = 0;
        int totalSeats = 0;
        for (SeatMapRow row : seatMap.rows()) {
            int freeInRow = 0;
            for (SeatMapTicket ticket : row.tickets()) {
                if (!soldSeats.contains(ticket.id())) {
                    freeInRow++;
                }
            }
            freeSeats += freeInRow;
            totalSeats += row.tickets().size();
            rows.add(new RowSeats(row.number(), freeInRow, row.tickets().size(), ticketTypes(row)));
        }
        rows.sort(Comparator.comparingInt(RowSeats::number));
        return new SeatOverview(seatMap.priceZones(), rows, freeSeats, totalSeats);
    }

    private String formatActivity(Activity activity, Hall hall, ActivityCategory category) {
        StringBuilder line = new StringBuilder();
        line.append("id=").append(activity.getId())
                .append(" | ").append(activity.getName())
                .append(" | ").append(activity.getDate())
                .append(' ').append(formatTime(activity.getStartTime()))
                .append('-').append(formatTime(activity.getEndTime()));
        if (hall != null) {
            line.append(" | ").append(hall.getCity()).append(", ").append(hall.getName());
        }
        if (category != null) {
            line.append(" | category: ").append(category.getName());
        }
        return line.toString();
    }

    private String formatPrices(List<PriceZoneInfo> priceZones) {
        if (priceZones == null || priceZones.isEmpty()) {
            return "no prices configured";
        }
        return priceZones.stream()
                .map(zone -> zone.type() + " " + formatPrice(zone.price()))
                .collect(Collectors.joining(", "));
    }

    private String formatPrice(Double price) {
        if (price == null) {
            return "unknown";
        }
        return BigDecimal.valueOf(price).stripTrailingZeros().toPlainString();
    }

    private String formatTime(LocalTime time) {
        return time == null ? "?" : time.format(TIME_FORMAT);
    }

    private List<String> ticketTypes(SeatMapRow row) {
        return row.tickets().stream()
                .map(ticket -> ticket.type().name())
                .distinct()
                .sorted()
                .toList();
    }

    private Map<UUID, Hall> hallsById(List<Activity> activities) {
        List<UUID> ids = activities.stream()
                .map(Activity::getHallId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return hallRepository.findAllByIds(ids).stream()
                .collect(Collectors.toMap(Hall::getId, hall -> hall));
    }

    private Map<UUID, ActivityCategory> categoriesById(List<Activity> activities) {
        List<UUID> ids = activities.stream()
                .map(Activity::getActivityCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return activityCategoryRepository.findAllByIds(ids).stream()
                .collect(Collectors.toMap(ActivityCategory::getId, category -> category));
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SeatlockUserDetails details) {
            return details.getId();
        }
        return null;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RowSeats(int number, int freeSeats, int totalSeats, List<String> ticketTypes) {
    }

    private record SeatOverview(List<PriceZoneInfo> priceZones, List<RowSeats> rows, int freeSeats, int totalSeats) {
    }
}
