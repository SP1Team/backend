package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.ActivityCategory;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ActivityCategoryRepository {

    private final DSLContext dsl;

    public List<ActivityCategory> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("activity_category")))
                .fetchInto(ActivityCategory.class);
    }

    public Optional<ActivityCategory> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("activity_category")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(ActivityCategory.class);
    }

    public Optional<ActivityCategory> findByName(String name) {
        return dsl.selectFrom(DSL.table(DSL.name("activity_category")))
                .where(DSL.field(DSL.name("name")).eq(name))
                .fetchOptionalInto(ActivityCategory.class);
    }

    public ActivityCategory save(ActivityCategory category) {
        if (category.getId() == null) {
            category.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("activity_category")))
                    .set(DSL.field(DSL.name("id"), UUID.class), category.getId())
                    .set(DSL.field(DSL.name("name"), String.class), category.getName())
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("activity_category")))
                    .set(DSL.field(DSL.name("name"), String.class), category.getName())
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(category.getId()))
                    .execute();
        }
        return category;
    }

    public void delete(ActivityCategory category) {
        dsl.deleteFrom(DSL.table(DSL.name("activity_category")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(category.getId()))
                .execute();
    }
}
