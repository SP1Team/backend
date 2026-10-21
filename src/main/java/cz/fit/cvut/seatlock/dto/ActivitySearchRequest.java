package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ActivitySearchRequest(
        @Size(max = 255, message = "Search text must not exceed 255 characters")
        String text,

        LocalDate date,
        LocalDate dateFrom,
        LocalDate dateTo,
        LocalTime startTime,

        @Size(max = 255, message = "Category name must not exceed 255 characters")
        String category,

        @Size(max = 255, message = "City name must not exceed 255 characters")
        String city
) {
    @AssertTrue(message = "dateFrom must be before or equal to dateTo")
    public boolean isDateRangeValid() {
        if (dateFrom == null || dateTo == null) return true;
        return !dateFrom.isAfter(dateTo);
    }
}
