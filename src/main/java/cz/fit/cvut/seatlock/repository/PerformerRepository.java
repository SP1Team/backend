package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Performer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerformerRepository extends JpaRepository<Performer, UUID> {
}