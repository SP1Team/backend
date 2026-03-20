package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Offer;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OfferRepository {

    private final DSLContext dsl;

    public List<Offer> findAll() {
        return dsl.selectFrom(DSL.table(DSL.name("offer")))
                .fetchInto(Offer.class);
    }

    public Optional<Offer> findById(UUID id) {
        return dsl.selectFrom(DSL.table(DSL.name("offer")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(id))
                .fetchOptionalInto(Offer.class);
    }

    public Offer save(Offer offer) {
        if (offer.getId() == null) {
            offer.setId(UUID.randomUUID());
            dsl.insertInto(DSL.table(DSL.name("offer")))
                    .set(DSL.field(DSL.name("id"), UUID.class), offer.getId())
                    .set(DSL.field(DSL.name("type"), String.class),
                            offer.getType() != null ? offer.getType().name() : null)
                    .set(DSL.field(DSL.name("price"), Double.class), offer.getPrice())
                    .set(DSL.field(DSL.name("activity_id"), UUID.class),
                            offer.getActivity() != null ? offer.getActivity().getId() : null)
                    .execute();
        } else {
            dsl.update(DSL.table(DSL.name("offer")))
                    .set(DSL.field(DSL.name("type"), String.class),
                            offer.getType() != null ? offer.getType().name() : null)
                    .set(DSL.field(DSL.name("price"), Double.class), offer.getPrice())
                    .set(DSL.field(DSL.name("activity_id"), UUID.class),
                            offer.getActivity() != null ? offer.getActivity().getId() : null)
                    .where(DSL.field(DSL.name("id"), UUID.class).eq(offer.getId()))
                    .execute();
        }
        return offer;
    }

    public void delete(Offer offer) {
        dsl.deleteFrom(DSL.table(DSL.name("offer")))
                .where(DSL.field(DSL.name("id"), UUID.class).eq(offer.getId()))
                .execute();
    }
}
