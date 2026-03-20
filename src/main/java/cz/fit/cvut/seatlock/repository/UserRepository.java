package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.User;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DSLContext dsl;

    public Optional<User> findUserByEmail(String email) {
        return dsl.selectFrom(DSL.table(DSL.name("app_user")))
                .where(DSL.field(DSL.name("email")).eq(email))
                .fetchOptionalInto(User.class);
    }

    public Optional<User> findUserByPhoneNumber(String phoneNumber) {
        return dsl.selectFrom(DSL.table(DSL.name("app_user")))
                .where(DSL.field(DSL.name("phone_number")).eq(phoneNumber))
                .fetchOptionalInto(User.class);
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("app_user")))
                    .set(DSL.field(DSL.name("id"), UUID.class), user.getId())
                    .set(DSL.field(DSL.name("email"), String.class), user.getEmail())
                    .set(DSL.field(DSL.name("password"), String.class), user.getPassword())
                    .set(DSL.field(DSL.name("name"), String.class), user.getName())
                    .set(DSL.field(DSL.name("surname"), String.class), user.getSurname())
                    .set(DSL.field(DSL.name("country_code"), String.class), user.getCountryCode())
                    .set(DSL.field(DSL.name("phone_number"), String.class), user.getPhoneNumber())
                    .set(DSL.field(DSL.name("role"), String.class), user.getRole() != null ? user.getRole().name() : null)
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("app_user")))
                    .set(DSL.field(DSL.name("email"), String.class), user.getEmail())
                    .set(DSL.field(DSL.name("password"), String.class), user.getPassword())
                    .set(DSL.field(DSL.name("name"), String.class), user.getName())
                    .set(DSL.field(DSL.name("surname"), String.class), user.getSurname())
                    .set(DSL.field(DSL.name("country_code"), String.class), user.getCountryCode())
                    .set(DSL.field(DSL.name("phone_number"), String.class), user.getPhoneNumber())
                    .set(DSL.field(DSL.name("role"), String.class), user.getRole() != null ? user.getRole().name() : null)
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(user.getId()))
                    .execute();
        }
        return user;
    }
}
