package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Seat;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SeatRepository {

    private final DSLContext dsl;

    public List<Seat> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("seat")))
                .fetchInto(Seat.class);
    }

    public Optional<Seat> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("seat")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(Seat.class);
    }

    public Seat save(Seat seat) {
        if (seat.getId() == null) {
            seat.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("seat")))
                    .set(DSL.field(DSL.name("id"), UUID.class), seat.getId())
                    .set(DSL.field(DSL.name("row_number"), Integer.class), seat.getRowNumber())
                    .set(DSL.field(DSL.name("seat_number"), Integer.class), seat.getSeatNumber())
                    .set(DSL.field(DSL.name("hall_id"), UUID.class),
                            seat.getHall() != null ? seat.getHall().getId() : null)
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("seat")))
                    .set(DSL.field(DSL.name("row_number"), Integer.class), seat.getRowNumber())
                    .set(DSL.field(DSL.name("seat_number"), Integer.class), seat.getSeatNumber())
                    .set(DSL.field(DSL.name("hall_id"), UUID.class),
                            seat.getHall() != null ? seat.getHall().getId() : null)
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(seat.getId()))
                    .execute();
        }
        return seat;
    }

    public void delete(Seat seat) {
        dsl.deleteFrom(DSL.table(DSL.name("seat")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(seat.getId()))
                .execute();
    }
}
