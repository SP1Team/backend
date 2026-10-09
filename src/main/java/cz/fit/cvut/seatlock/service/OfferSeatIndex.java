package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Offer;
import cz.fit.cvut.seatlock.domain.Seat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class OfferSeatIndex {

    private OfferSeatIndex() {
    }

    static Map<UUID, Offer> create(Collection<Offer> offers) {
        Map<UUID, Offer> offerBySeatId = new HashMap<>();

        for (Offer offer : offers) {
            for (Seat seat : offer.getSeats()) {
                Offer previousOffer = offerBySeatId.putIfAbsent(seat.getId(), offer);
                if (previousOffer != null) {
                    throw new IllegalArgumentException(
                            "Seat is assigned to multiple offers: " + seat.getId()
                    );
                }
            }
        }

        return offerBySeatId;
    }
}
