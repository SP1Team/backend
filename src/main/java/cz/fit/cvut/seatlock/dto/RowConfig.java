package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.Min;

public record RowConfig(
        @Min(value = 1, message = "Row number must be at least 1")
        int rowNumber,

        @Min(value = 1, message = "Count of seats must be at least 1")
        int countOfSeats
) {
}
