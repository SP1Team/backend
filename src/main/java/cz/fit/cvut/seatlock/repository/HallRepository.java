package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Hall;
import static cz.fit.cvut.seatlock.generated.Tables.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HallRepository {

    private final DSLContext dsl;

    public List<Hall> findAll() {
        return dsl.selectFrom(HALL)
                .fetchInto(Hall.class);
    }

    public Optional<Hall> findById(UUID id) {
        return dsl.selectFrom(HALL)
                .where(HALL.ID.eq(id))
                .fetchOptionalInto(Hall.class);
    }

    public Hall save(Hall hall) {
        var record = dsl.newRecord(HALL, hall);
        if(hall.getId() == null){
            record.setId(UUID.randomUUID());
        }
        record.store();
        return record.into(Hall.class);
    }

    public void delete(Hall hall) {
        dsl.deleteFrom(HALL)
                .where(HALL.ID.eq(hall.getId()))
                .execute();
    }
}
