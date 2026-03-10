package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.ActivityCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityCategoryRepository extends JpaRepository<ActivityCategory, UUID> {
}