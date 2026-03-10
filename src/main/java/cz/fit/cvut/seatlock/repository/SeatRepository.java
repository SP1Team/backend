package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
}