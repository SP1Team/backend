package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Performer;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PerformerRepository {

    private final DSLContext dsl;

    public List<Performer> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("performer")))
                .fetchInto(Performer.class);
    }

    public Optional<Performer> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("performer")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(Performer.class);
    }

    public Performer save(Performer performer) {
        if (performer.getId() == null) {
            performer.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("performer")))
                    .set(DSL.field(DSL.name("id"), UUID.class), performer.getId())
                    .set(DSL.field(DSL.name("name"), String.class), performer.getName())
                    .set(DSL.field(DSL.name("description"), String.class), performer.getDescription())
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("performer")))
                    .set(DSL.field(DSL.name("name"), String.class), performer.getName())
                    .set(DSL.field(DSL.name("description"), String.class), performer.getDescription())
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(performer.getId()))
                    .execute();
        }
        return performer;
    }

    public void delete(Performer performer) {
        dsl.deleteFrom(DSL.table(DSL.name("performer")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(performer.getId()))
                .execute();
    }
}
