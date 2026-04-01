package cz.fit.cvut.seatlock.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationEngine {

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);
    private static final Duration RESERVATION_PROLONG_BY = Duration.ofMinutes(5);
    private static final long LOCK_WAIT_SECONDS = 2;

    private final RedissonClient redissonClient;

    public void reserveSeat(UUID seatId, UUID userId) {
        String lockKey = "lock:seat:" + seatId;
        // технический ключ для распределённого лока: пока он удерживается, только один поток
        // во всём кластере может выполнять операции с этим seatId

        String reservationKey = "reservation:seat:" + seatId;
        // ключ бизнес-резерва: по нему в Redis хранится сам объект брони (кто и когда забронировал)

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS); // пытаемся захватить лок в течение двух секунд
            if (!locked) { // если не получилось захватить лок, значит другой поток обрабатывает уже это место
                throw new IllegalStateException("Seat is being processed by another user");
            }
            // место находится под локом с этого момента
            // только один поток во всём кластере может быть здесь для этого seatId

            RBucket<SeatReservation> bucket = redissonClient.getBucket(reservationKey);
            // получаем объект резервации места, если он был уже в редис по этому ключу
            SeatReservation existing = bucket.get();
            if (existing != null) {
                // уже есть активная бронь в Redis
                throw new IllegalStateException("Seat is already reserved");
            }

            SeatReservation reservation = new SeatReservation(
                    seatId,
                    userId,
                    Instant.now()
            ); // создаем новый объект резервации места

            // создаём бронь с TTL = 10 минут
            bucket.set(reservation, RESERVATION_TTL);
            // После вызова `set` Redis сам удалит ключ `reservation:seat:{id}` через 10 минут.
            // Никаких дополнительных cron‑задач для очистки резервов не нужно.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock(); // теперь другие потоки могут захватить это место тоже
            }
        }
    }

    public void cancelReservation(UUID seatId, UUID userId) {
        String lockKey = "lock:seat:" + seatId;
        String reservationKey = "reservation:seat:" + seatId;

        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Seat is being processed by another user");
            }

            RBucket<SeatReservation> bucket = redissonClient.getBucket(reservationKey);
            SeatReservation existing = bucket.get();

            if (existing == null) {
                // Брони уже нет (TTL истёк или бронь уже была превращена в покупку)
                return;
            }

            if (!existing.userId().equals(userId)) {
                // Текущий пользователь не владелец брони — нельзя отменять чужую бронь
                throw new IllegalStateException("Current user does not own this reservation");
            }

            // Всё ок — удаляем бронь
            bucket.delete();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void prolongReservation(UUID seatId, UUID userId) {
        String lockKey = "lock:seat:" + seatId;
        String reservationKey = "reservation:seat:" + seatId;

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

            // сколько осталось жить ключу сейчас (мс)
            long ttlMillis = bucket.remainTimeToLive();
            if (ttlMillis <= 0) {
                throw new IllegalStateException("Reservation is already expired");
            }

            // прибавляем 5 минут
            long newTtlMillis = ttlMillis + RESERVATION_PROLONG_BY.toMillis();

            // задаём новый TTL
            bucket.expire(Duration.ofMillis(newTtlMillis));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void reserveMultipleSeats(List<UUID> seatIds, UUID userId) {
        // сортируем, чтобы порядок всегда был одинаков
        var sortedSeatIds = seatIds.stream().sorted().toList();

        RLock[] locks = sortedSeatIds.stream()
                .map(id -> redissonClient.getLock("lock:seat:" + id))
                .toArray(RLock[]::new);
        // создаем массив локов на каждое место по отдельности

        var multiLock = redissonClient.getMultiLock(locks);
        // создаем мультилок на все места сразу
        try {
            boolean locked = multiLock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("Some seats are being processed by another user");
            }

            // критическая секция для всех seatIds

            // сначала проверяем, что ни одно из мест не зарезервировано
            for (UUID seatId : sortedSeatIds) {
                String reservationKey = "reservation:seat:" + seatId;
                var bucket = redissonClient.<SeatReservation>getBucket(reservationKey);
                SeatReservation existing = bucket.get();
                if (existing != null) {
                    throw new IllegalStateException("Seat " + seatId + " is already reserved");
                }
            }

            // если все места свободны — создаём брони на каждое
            for (UUID seatId : sortedSeatIds) {
                String reservationKey = "reservation:seat:" + seatId;
                var bucket = redissonClient.<SeatReservation>getBucket(reservationKey);

                SeatReservation reservation = new SeatReservation(
                        seatId,
                        userId,
                        Instant.now()
                );

                bucket.set(reservation, RESERVATION_TTL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring multi-lock", e);
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }
}
