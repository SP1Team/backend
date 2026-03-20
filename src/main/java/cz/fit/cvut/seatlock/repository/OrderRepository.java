package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Order;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final DSLContext dsl;

    public List<Order> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("app_order")))
                .fetchInto(Order.class);
    }

    public Optional<Order> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("app_order")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(Order.class);
    }

    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("app_order")))
                    .set(DSL.field(DSL.name("id"), UUID.class), order.getId())
                    .set(DSL.field(DSL.name("total_price"), Double.class), order.getTotalPrice())
                    .set(DSL.field(DSL.name("status"), String.class),
                            order.getStatus() != null ? order.getStatus().name() : null)
                    .set(DSL.field(DSL.name("created_at"), LocalDateTime.class), order.getCreatedAt())
                    .set(DSL.field(DSL.name("user_id"), UUID.class),
                            order.getUser() != null ? order.getUser().getId() : null)
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("app_order")))
                    .set(DSL.field(DSL.name("total_price"), Double.class), order.getTotalPrice())
                    .set(DSL.field(DSL.name("status"), String.class),
                            order.getStatus() != null ? order.getStatus().name() : null)
                    .set(DSL.field(DSL.name("created_at"), LocalDateTime.class), order.getCreatedAt())
                    .set(DSL.field(DSL.name("user_id"), UUID.class),
                            order.getUser() != null ? order.getUser().getId() : null)
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(order.getId()))
                    .execute();
        }
        return order;
    }

    public void delete(Order order) {
        dsl.deleteFrom(DSL.table(DSL.name("app_order")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(order.getId()))
                .execute();
    }
}
