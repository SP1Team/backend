package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketTypeInput(
        @NotBlank(message = "Ticket type name must not be blank")
        String name,

        @NotNull(message = "Price must not be null")
        @Positive(message = "Price must be positive")
        Double price
) {}
