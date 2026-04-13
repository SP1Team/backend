package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Order;
import cz.fit.cvut.seatlock.domain.User;
import static cz.fit.cvut.seatlock.generated.Tables.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final DSLContext dsl;

    public List<Order> findAll() {
        return dsl.selectFrom(APP_ORDER)
                .fetchInto(Order.class);
    }

    public Optional<Order> findById(UUID id) {
        return dsl.selectFrom(APP_ORDER)
                .where(APP_ORDER.ID.eq(id))
                .fetchOptional(r -> {
                    Order o = r.into(Order.class);
                    User user = new User();
                    user.setId(r.get(APP_ORDER.USER_ID));
                    o.setUser(user);
                    return o;
                });
    }

    public Order save(Order order) {
        var record = dsl.newRecord(APP_ORDER, order);
        if (order.getStatus() != null) {
            record.setStatus(order.getStatus().name());
        }
        if (order.getUser() != null) {
            record.setUserId(order.getUser().getId());
        }
        if (order.getId() == null) {
            record.setId(UUID.randomUUID());
        }
        record.store();
        return record.into(Order.class);
    }

    public List<Order> findByUserId(UUID userId) {
        return dsl.selectFrom(APP_ORDER)
                .where(APP_ORDER.USER_ID.eq(userId))
                .orderBy(APP_ORDER.CREATED_AT.desc())
                .fetch(r -> {
                    Order o = r.into(Order.class);
                    User user = new User();
                    user.setId(r.get(APP_ORDER.USER_ID));
                    o.setUser(user);
                    return o;
                });
    }

    public void delete(Order order) {
        dsl.deleteFrom(APP_ORDER)
                .where(APP_ORDER.ID.eq(order.getId()))
                .execute();
    }
}
