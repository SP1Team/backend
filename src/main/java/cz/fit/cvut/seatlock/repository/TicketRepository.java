package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
}