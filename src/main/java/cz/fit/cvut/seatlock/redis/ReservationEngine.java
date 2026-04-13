package cz.fit.cvut.seatlock.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationEngine {

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);
    private static final Duration RESERVATION_PROLONG_BY = Duration.ofMinutes(5);
    private static final long LOCK_WAIT_SECONDS = 2;

    private final RedissonClient redissonClient;
    private final cz.fit.cvut.seatlock.repository.TicketRepository ticketRepository;

    private String getReservationKey(UUID activityId, UUID seatId) {
        return "reservation:activity:" + activityId + ":seat:" + seatId;
    }

    private String getLockKey(UUID activityId, UUID seatId) {
        return "lock:activity:" + activityId + ":seat:" + seatId;
    }

    private String getCartKey(UUID userId) {
        return "cart:user:" + userId;
    }


    public void cancelReservation(UUID activityId, UUID seatId, UUID userId) {
        String lockKey = getLockKey(activityId, seatId);
        String reservationKey = getReservationKey(activityId, seatId);

        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Seat is being processed by another user");
            }

            RBucket<SeatReservation> bucket = redissonClient.getBucket(reservationKey);
            SeatReservation existing = bucket.get();

            if (existing == null) {
                return;
            }

            if (!existing.userId().equals(userId)) {
                throw new IllegalStateException("Current user does not own this reservation");
            }

            bucket.delete();
            // Remove from cart
            RSet<SeatReservation> cart = redissonClient.getSet(getCartKey(userId));
            cart.remove(existing);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void prolongReservation(UUID activityId, UUID seatId, UUID userId) {
        String lockKey = getLockKey(activityId, seatId);
        String reservationKey = getReservationKey(activityId, seatId);

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Seat is being processed by another user");
            }

            RBucket<SeatReservation> bucket = redissonClient.getBucket(reservationKey);
            SeatReservation existing = bucket.get();

            if (existing == null) {
                throw new IllegalStateException("No active reservation to prolong");
            }

            if (!existing.userId().equals(userId)) {
                throw new IllegalStateException("Current user does not own this reservation");
            }

            long ttlMillis = bucket.remainTimeToLive();
            if (ttlMillis <= 0) {
                throw new IllegalStateException("Reservation is already expired");
            }

            long newTtlMillis = ttlMillis + RESERVATION_PROLONG_BY.toMillis();
            bucket.expire(Duration.ofMillis(newTtlMillis));

            // Update cart TTL
            RSet<SeatReservation> cart = redissonClient.getSet(getCartKey(userId));
            cart.expire(Duration.ofMillis(newTtlMillis));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void reserveMultipleSeats(UUID activityId, List<UUID> seatIds, UUID userId) {
        var sortedSeatIds = seatIds.stream().sorted().toList();

        RLock[] locks = sortedSeatIds.stream()
                .map(id -> redissonClient.getLock(getLockKey(activityId, id)))
                .toArray(RLock[]::new);

        var multiLock = redissonClient.getMultiLock(locks);
        try {
            boolean locked = multiLock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Some seats are being processed by another user");
            }

            for (UUID seatId : sortedSeatIds) {
                String reservationKey = getReservationKey(activityId, seatId);
                var bucket = redissonClient.<SeatReservation>getBucket(reservationKey);
                SeatReservation existing = bucket.get();
                if (existing != null) {
                    throw new IllegalStateException("Seat " + seatId + " is already reserved in Redis");
                }
            }

            // DB check
            var dbStatuses = ticketRepository.findStatusesByActivityId(activityId);
            for (UUID seatId : sortedSeatIds) {
                if (dbStatuses.get(seatId) == cz.fit.cvut.seatlock.domain.enums.TicketStatus.SOLD) {
                    throw new IllegalStateException("Seat " + seatId + " is already sold in DB");
                }
            }

            RSet<SeatReservation> cart = redissonClient.getSet(getCartKey(userId));
            for (UUID seatId : sortedSeatIds) {
                String reservationKey = getReservationKey(activityId, seatId);
                var bucket = redissonClient.<SeatReservation>getBucket(reservationKey);

                SeatReservation reservation = new SeatReservation(
                        seatId,
                        activityId,
                        userId,
                        Instant.now()
                );

                bucket.set(reservation, RESERVATION_TTL);
                cart.add(reservation);
            }
            cart.expire(RESERVATION_TTL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring multi-lock", e);
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    public List<SeatReservation> getCart(UUID userId) {
        RSet<SeatReservation> cartSet = redissonClient.getSet(getCartKey(userId));
        Set<SeatReservation> rawCart = cartSet.readAll();

        List<SeatReservation> validReservations = new ArrayList<>();
        List<SeatReservation> expiredReservations = new ArrayList<>();

        for (SeatReservation res : rawCart) {
            String reservationKey = getReservationKey(res.activityId(), res.seatId());
            if (redissonClient.getBucket(reservationKey).isExists()) {
                validReservations.add(res);
            } else {
                expiredReservations.add(res);
            }
        }

        // clean up expired items from the cart
        if (!expiredReservations.isEmpty()) {
            expiredReservations.forEach(cartSet::remove);
        }

        return validReservations;
    }

    public void clearCartForActivity(UUID userId, UUID activityId) {
        RSet<SeatReservation> cartSet = redissonClient.getSet(getCartKey(userId));
        Set<SeatReservation> rawCart = cartSet.readAll();

        List<SeatReservation> toRemove = rawCart.stream()
                .filter(res -> res.activityId().equals(activityId))
                .toList();

        for (SeatReservation res : toRemove) {
            String reservationKey = getReservationKey(res.activityId(), res.seatId());
            redissonClient.getBucket(reservationKey).delete();
        }

        toRemove.forEach(cartSet::remove);
    }

    public Set<UUID> getReservedSeatIds(UUID activityId) {
        String pattern = "reservation:activity:" + activityId + ":seat:*";
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(pattern);
        Set<UUID> reservedSeatIds = new java.util.HashSet<>();
        for (String key : keys) {
            String[] parts = key.split(":");
            reservedSeatIds.add(UUID.fromString(parts[parts.length - 1]));
        }
        return reservedSeatIds;
    }
}
