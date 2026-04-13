package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.redis.ReservationEngine;
import cz.fit.cvut.seatlock.redis.SeatReservation;
import cz.fit.cvut.seatlock.security.SeatlockUserDetails;
import cz.fit.cvut.seatlock.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ReservationResolver {

    private final ReservationEngine reservationEngine;
    private final OrderService orderService;

    private UUID getCurrentUserId() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SeatlockUserDetails details) {
            return details.getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    @QueryMapping
    public List<SeatReservation> getCart() {
        return reservationEngine.getCart(getCurrentUserId());
    }

    @MutationMapping
    public Boolean reserveSeats(@Argument UUID activityId, @Argument List<UUID> seatIds) {
        reservationEngine.reserveMultipleSeats(activityId, seatIds, getCurrentUserId());
        return true;
    }

    @MutationMapping
    public Boolean cancelReservation(@Argument UUID activityId, @Argument UUID seatId) {
        reservationEngine.cancelReservation(activityId, seatId, getCurrentUserId());
        return true;
    }

    @MutationMapping
    public Boolean checkout(@Argument UUID activityId) {
        return orderService.checkout(getCurrentUserId(), activityId);
    }
}
