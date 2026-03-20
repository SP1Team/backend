package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Activity;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ActivityRepository {

    private final DSLContext dsl;

    public List<Activity> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("activity")))
                .fetchInto(Activity.class);
    }

    public Optional<Activity> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("activity")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(Activity.class);
    }

    public Activity save(Activity activity) {
        if (activity.getId() == null) {
            activity.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("activity")))
                    .set(DSL.field(DSL.name("id"), UUID.class), activity.getId())
                    .set(DSL.field(DSL.name("name"), String.class), activity.getName())
                    .set(DSL.field(DSL.name("date"), java.time.LocalDate.class), activity.getDate())
                    .set(DSL.field(DSL.name("start_time"), java.time.LocalTime.class), activity.getStartTime())
                    .set(DSL.field(DSL.name("end_time"), java.time.LocalTime.class), activity.getEndTime())
                    .set(DSL.field(DSL.name("description"), String.class), activity.getDescription())
                    .set(DSL.field(DSL.name("activity_category_id"), UUID.class),
                            activity.getCategory() != null ? activity.getCategory().getId() : null)
                    .set(DSL.field(DSL.name("hall_id"), UUID.class),
                            activity.getHall() != null ? activity.getHall().getId() : null)
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("activity")))
                    .set(DSL.field(DSL.name("name"), String.class), activity.getName())
                    .set(DSL.field(DSL.name("date"), java.time.LocalDate.class), activity.getDate())
                    .set(DSL.field(DSL.name("start_time"), java.time.LocalTime.class), activity.getStartTime())
                    .set(DSL.field(DSL.name("end_time"), java.time.LocalTime.class), activity.getEndTime())
                    .set(DSL.field(DSL.name("description"), String.class), activity.getDescription())
                    .set(DSL.field(DSL.name("activity_category_id"), UUID.class),
                            activity.getCategory() != null ? activity.getCategory().getId() : null)
                    .set(DSL.field(DSL.name("hall_id"), UUID.class),
                            activity.getHall() != null ? activity.getHall().getId() : null)
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(activity.getId()))
                    .execute();
        }
        return activity;
    }

    public void delete(Activity activity) {
        dsl.deleteFrom(DSL.table(DSL.name("activity")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(activity.getId()))
                .execute();
    }
}
