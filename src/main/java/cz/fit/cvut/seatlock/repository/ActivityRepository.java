package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
}