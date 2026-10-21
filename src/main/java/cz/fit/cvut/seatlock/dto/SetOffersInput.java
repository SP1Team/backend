package cz.fit.cvut.seatlock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SetOffersInput(
        @NotEmpty(message = "Ticket types must not be empty")
        List<@Valid TicketTypeInput> types,

        @NotEmpty(message = "Row offers must not be empty")
        List<@Valid RowOfferInput> offers
) {}
