package cz.fit.cvut.seatlock.dto;

import cz.fit.cvut.seatlock.domain.enums.TicketType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record PriceZoneRequest(
        @NotNull(message = "Ticket type must not be null")
        TicketType ticketType,

        @NotNull(message = "Price must not be null")
        @Positive(message = "Price must be positive")
        Double price,

        List<@NotNull(message = "Seat ID must not be null") UUID> seatIds
) {
}
