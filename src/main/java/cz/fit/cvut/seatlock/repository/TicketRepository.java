package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Ticket;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TicketRepository {

    private final DSLContext dsl;

    public List<Ticket> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("ticket")))
                .fetchInto(Ticket.class);
    }

    public Optional<Ticket> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("ticket")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(Ticket.class);
    }

    public Ticket save(Ticket ticket) {
        if (ticket.getId() == null) {
            ticket.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("ticket")))
                    .set(DSL.field(DSL.name("id"), UUID.class), ticket.getId())
                    .set(DSL.field(DSL.name("status"), String.class),
                            ticket.getStatus() != null ? ticket.getStatus().name() : null)
                    .set(DSL.field(DSL.name("order_id"), UUID.class),
                            ticket.getOrder() != null ? ticket.getOrder().getId() : null)
                    .set(DSL.field(DSL.name("seat_id"), UUID.class),
                            ticket.getSeat() != null ? ticket.getSeat().getId() : null)
                    .set(DSL.field(DSL.name("offer_id"), UUID.class),
                            ticket.getOffer() != null ? ticket.getOffer().getId() : null)
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("ticket")))
                    .set(DSL.field(DSL.name("status"), String.class),
                            ticket.getStatus() != null ? ticket.getStatus().name() : null)
                    .set(DSL.field(DSL.name("order_id"), UUID.class),
                            ticket.getOrder() != null ? ticket.getOrder().getId() : null)
                    .set(DSL.field(DSL.name("seat_id"), UUID.class),
                            ticket.getSeat() != null ? ticket.getSeat().getId() : null)
                    .set(DSL.field(DSL.name("offer_id"), UUID.class),
                            ticket.getOffer() != null ? ticket.getOffer().getId() : null)
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(ticket.getId()))
                    .execute();
        }
        return ticket;
    }

    public void delete(Ticket ticket) {
        dsl.deleteFrom(DSL.table(DSL.name("ticket")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(ticket.getId()))
                .execute();
    }
}
