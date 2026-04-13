package cz.fit.cvut.seatlock.redis;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record SeatReservation(
        UUID seatId,
        UUID activityId,
        UUID userId,
        Instant createdAt
) implements Serializable {
}
