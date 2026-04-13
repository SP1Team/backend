package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.*;
import cz.fit.cvut.seatlock.domain.enums.OrderStatus;
import cz.fit.cvut.seatlock.domain.enums.TicketStatus;
import cz.fit.cvut.seatlock.domain.enums.TicketType;
import cz.fit.cvut.seatlock.redis.ReservationEngine;
import cz.fit.cvut.seatlock.redis.SeatReservation;
import cz.fit.cvut.seatlock.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final OfferRepository offerRepository;
    private final SeatRepository seatRepository;
    private final ActivityRepository activityRepository;
    private final ReservationEngine reservationEngine;

    @Transactional
    public Boolean checkout(UUID userId, UUID activityId) {
        List<SeatReservation> cart = reservationEngine.getCart(userId);
        List<SeatReservation> activityReservations = cart.stream()
                .filter(res -> res.activityId().equals(activityId))
                .toList();

        if (activityReservations.isEmpty()) {
            throw new RuntimeException("No reservations found for this activity in your cart");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Offer> offers = offerRepository.findByActivityId(activityId);
        Offer offer;
        if (offers.isEmpty()) {
            // todo удалить создание бесплатного оффера во время третьего спринта
            Activity activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new RuntimeException("Activity not found: " + activityId));
            
            offer = new Offer();
            offer.setActivity(activity);
            offer.setPrice(0.0);
            offer.setType(TicketType.STANDARD);
            offer = offerRepository.save(offer);
        } else {
            offer = offers.getFirst();
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalPrice(offer.getPrice() * activityReservations.size());
        Order savedOrder = orderRepository.save(order);

        for (SeatReservation res : activityReservations) {
            var statuses = ticketRepository.findStatusesByActivityId(activityId);
            if (statuses.get(res.seatId()) == TicketStatus.SOLD) {
                throw new RuntimeException("Seat " + res.seatId() + " is already sold");
            }

            Seat seat = seatRepository.findById(res.seatId())
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + res.seatId()));

            Ticket ticket = new Ticket();
            ticket.setOrder(savedOrder);
            ticket.setSeat(seat);
            ticket.setOffer(offer);
            ticket.setStatus(TicketStatus.SOLD);
            ticketRepository.save(ticket);
        }

        reservationEngine.clearCartForActivity(userId, activityId);

        return true;
    }
}
