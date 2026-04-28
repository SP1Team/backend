package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.domain.Order;
import cz.fit.cvut.seatlock.domain.Seat;
import cz.fit.cvut.seatlock.domain.Ticket;
import cz.fit.cvut.seatlock.dto.CheckoutResponse;
import cz.fit.cvut.seatlock.repository.SeatRepository;
import cz.fit.cvut.seatlock.security.SeatlockUserDetails;
import cz.fit.cvut.seatlock.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class OrderResolver {

    private final OrderService orderService;
    private final SeatRepository seatRepository;

    private UUID getCurrentUserId() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SeatlockUserDetails details) {
            return details.getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    @QueryMapping
    public List<Order> getOrders() {
        return orderService.getOrdersByUserId(getCurrentUserId());
    }

    @QueryMapping
    public Order getOrderById(@Argument UUID id) {
        Order order = orderService.getOrderById(id);
        // Security check: only owner can see the order
        if (!order.getUser().getId().equals(getCurrentUserId())) {
            throw new RuntimeException("Access denied");
        }
        return order;
    }

    @SchemaMapping(typeName = "Order", field = "tickets")
    public List<Ticket> tickets(Order order) {
        return orderService.getTicketsByOrderId(order.getId());
    }

    @SchemaMapping(typeName = "Ticket", field = "seatNumber")
    public int seatNumber(Ticket ticket) {
        return seatRepository.findById(ticket.getSeat().getId())
                .map(Seat::getSeatNumber)
                .orElse(0);
    }

    @SchemaMapping(typeName = "Ticket", field = "rowNumber")
    public int rowNumber(Ticket ticket) {
        return seatRepository.findById(ticket.getSeat().getId())
                .map(Seat::getRowNumber)
                .orElse(0);
    }

    @MutationMapping
    public CheckoutResponse checkout(@Argument UUID activityId, @Argument List<UUID> seatIds) {
        return orderService.checkout(getCurrentUserId(), activityId, seatIds);
    }
}
