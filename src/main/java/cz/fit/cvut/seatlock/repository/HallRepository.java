package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Hall;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HallRepository {

    private final DSLContext dsl;

    public List<Hall> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("hall")))
                .fetchInto(Hall.class);
    }

    public Optional<Hall> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("hall")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(Hall.class);
    }

    public Hall save(Hall hall) {
        if (hall.getId() == null) {
            hall.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("hall")))
                    .set(DSL.field(DSL.name("id"), UUID.class), hall.getId())
                    .set(DSL.field(DSL.name("name"), String.class), hall.getName())
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("hall")))
                    .set(DSL.field(DSL.name("name"), String.class), hall.getName())
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(hall.getId()))
                    .execute();
        }
        return hall;
    }

    public void delete(Hall hall) {
        dsl.deleteFrom(DSL.table(DSL.name("hall")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(hall.getId()))
                .execute();
    }
}
