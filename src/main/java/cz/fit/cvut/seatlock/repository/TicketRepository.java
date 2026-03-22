package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Ticket;
import static cz.fit.cvut.seatlock.generated.Tables.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TicketRepository {

    private final DSLContext dsl;

    public List<Ticket> findAll() {
        return dsl.selectFrom(TICKET)
                .fetchInto(Ticket.class);
    }

    public Optional<Ticket> findById(UUID id) {
        return dsl.selectFrom(TICKET)
                .where(TICKET.ID.eq(id))
                .fetchOptionalInto(Ticket.class);
    }

    public Ticket save(Ticket ticket) {
        var record = dsl.newRecord(TICKET, ticket);
        if (ticket.getStatus() != null) {
            record.setStatus(ticket.getStatus().name());
        }
        if (ticket.getOrder() != null) {
            record.setOrderId(ticket.getOrder().getId());
        }
        if (ticket.getSeat() != null) {
            record.setSeatId(ticket.getSeat().getId());
        }
        if (ticket.getOffer() != null) {
            record.setOfferId(ticket.getOffer().getId());
        }
        if (ticket.getId() == null) {
            record.setId(UUID.randomUUID());
        }
        record.store();
        return record.into(Ticket.class);
    }

    public void delete(Ticket ticket) {
        dsl.deleteFrom(TICKET)
                .where(TICKET.ID.eq(ticket.getId()))
                .execute();
    }
}
