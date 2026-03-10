package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HallRepository extends JpaRepository<Hall, UUID> {
}