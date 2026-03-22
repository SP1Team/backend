package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Performer;
import static cz.fit.cvut.seatlock.generated.Tables.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PerformerRepository {

    private final DSLContext dsl;

    public List<Performer> findAll() {
        return dsl.selectFrom(PERFORMER)
                .fetchInto(Performer.class);
    }

    public Optional<Performer> findById(UUID id) {
        return dsl.selectFrom(PERFORMER)
                .where(PERFORMER.ID.eq(id))
                .fetchOptionalInto(Performer.class);
    }

    public Performer save(Performer performer) {
        var record = dsl.newRecord(PERFORMER, performer);
        if (performer.getId() == null) {
            record.setId(UUID.randomUUID());
        }
        record.store();
        return record.into(Performer.class);
    }

    public void delete(Performer performer) {
        dsl.deleteFrom(PERFORMER)
                .where(PERFORMER.ID.eq(performer.getId()))
                .execute();
    }
}
