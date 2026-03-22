package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.Offer;
import static cz.fit.cvut.seatlock.generated.Tables.*;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OfferRepository {

    private final DSLContext dsl;

    public List<Offer> findAll() {
        return dsl.selectFrom(OFFER)
                .fetchInto(Offer.class);
    }

    public Optional<Offer> findById(UUID id) {
        return dsl.selectFrom(OFFER)
                .where(OFFER.ID.eq(id))
                .fetchOptionalInto(Offer.class);
    }

    public Offer save(Offer offer) {
        var record = dsl.newRecord(OFFER, offer);
        if (offer.getType() != null) {
            record.setType(offer.getType().name());
        }
        if (offer.getActivity() != null) {
            record.setActivityId(offer.getActivity().getId());
        }
        if (offer.getId() == null) {
            record.setId(UUID.randomUUID());
        }
        record.store();
        return record.into(Offer.class);
    }

    public void delete(Offer offer) {
        dsl.deleteFrom(OFFER)
                .where(OFFER.ID.eq(offer.getId()))
                .execute();
    }
}
