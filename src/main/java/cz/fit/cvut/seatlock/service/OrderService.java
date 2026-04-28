package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.*;
import cz.fit.cvut.seatlock.domain.enums.OrderStatus;
import cz.fit.cvut.seatlock.domain.enums.TicketStatus;
import cz.fit.cvut.seatlock.domain.enums.TicketType;
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

    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Ticket> getTicketsByOrderId(UUID orderId) {
        return ticketRepository.findByOrderId(orderId);
    }

    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public Boolean checkout(UUID userId, UUID activityId) {
        // TODO: Integration with Go service via gRPC to get cart/reservations
        throw new UnsupportedOperationException("Checkout is temporarily disabled until Go service integration is complete");
    }
}
