package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Activity;
import static cz.fit.cvut.seatlock.generated.Tables.*;

import cz.fit.cvut.seatlock.domain.Performer;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
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

    public Optional<Activity> findByName(String name) {
        return dsl.selectFrom(ACTIVITY)
                .where(ACTIVITY.NAME.eq(name))
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

    public void savePerformers(UUID activityId, List<Performer> performers) {
        var batch = performers.stream()
                .map(performer -> dsl.insertInto(ACTIVITY_PERFORMER)
                        .set(ACTIVITY_PERFORMER.ACTIVITY_ID, activityId)
                        .set(ACTIVITY_PERFORMER.PERFORMER_ID, performer.getId()))
                .toList();
        dsl.batch(batch).execute();
    }

    public void deletePerformers(UUID activityId) {
        dsl.deleteFrom(ACTIVITY_PERFORMER)
                .where(ACTIVITY_PERFORMER.ACTIVITY_ID.eq(activityId))
                .execute();
    }

    public void deletePerformerFromActivities(UUID performerId) {
        dsl.deleteFrom(ACTIVITY_PERFORMER)
                .where(ACTIVITY_PERFORMER.PERFORMER_ID.eq(performerId))
                .execute();
    }

    public void delete(Activity activity) {
        dsl.deleteFrom(ACTIVITY)
                .where(ACTIVITY.ID.eq(activity.getId()))
                .execute();
    }

    public boolean availableTimeForActivity(UUID hallId, LocalDate date, LocalTime start, LocalTime end, UUID excludeId) {
        var condition = ACTIVITY.HALL_ID.eq(hallId)
                .and(ACTIVITY.DATE.eq(date))
                .and(ACTIVITY.START_TIME.lt(end))
                .and(ACTIVITY.END_TIME.gt(start));

        if (excludeId != null) {
            condition = condition.and(ACTIVITY.ID.ne(excludeId));
        }

        return !dsl.fetchExists(dsl.selectOne().from(ACTIVITY).where(condition));
    }

    public List<UUID> findPerformerIdsByActivityId(UUID activityId) {
        return dsl.select(ACTIVITY_PERFORMER.PERFORMER_ID)
                .from(ACTIVITY_PERFORMER)
                .where(ACTIVITY_PERFORMER.ACTIVITY_ID.eq(activityId))
                .fetchInto(UUID.class);
    }

    public List<Activity> findAllByIds(List<UUID> ids) {
        return dsl.selectFrom(ACTIVITY)
                .where(ACTIVITY.ID.in(ids))
                .fetchInto(Activity.class);
    }
}
