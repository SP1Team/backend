package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RowOfferInput(
        @NotNull(message = "Row number must not be null")
        @Min(value = 1, message = "Row number must be at least 1")
        Integer row,

        @NotBlank(message = "Ticket type name must not be blank")
        String typeName
) {}
