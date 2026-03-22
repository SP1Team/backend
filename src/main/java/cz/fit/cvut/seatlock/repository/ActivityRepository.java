package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Activity;
import static cz.fit.cvut.seatlock.generated.Tables.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ActivityRepository {

    private final DSLContext dsl;

    public List<Activity> findAll() {
        return dsl.selectFrom(ACTIVITY)
                .fetchInto(Activity.class);
    }

    public Optional<Activity> findById(UUID id) {
        return dsl.selectFrom(ACTIVITY)
                .where(ACTIVITY.ID.eq(id))
                .fetchOptionalInto(Activity.class);
    }

    public Activity save(Activity activity) {
        var record = dsl.newRecord(ACTIVITY, activity);

        if (activity.getCategory() != null) {
            record.setActivityCategoryId(activity.getCategory().getId());
        }
        if (activity.getHall() != null) {
            record.setHallId(activity.getHall().getId());
        }

        if (record.getId() == null) {
            record.setId(UUID.randomUUID());
        }

        record.store();
        return record.into(Activity.class);
    }

    public void delete(Activity activity) {
        dsl.deleteFrom(ACTIVITY)
                .where(ACTIVITY.ID.eq(activity.getId()))
                .execute();
    }
}
