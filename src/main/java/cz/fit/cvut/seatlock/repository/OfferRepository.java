package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
}