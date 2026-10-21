package cz.fit.cvut.seatlock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ActivityRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Activity name must not be blank")
        @Size(max = 255, message = "Activity name must not exceed 255 characters")
        String name,

        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        UUID categoryId,
        UUID hallId,
        List<UUID> performer,
        List<@Valid PriceZoneRequest> priceZones
) {
    @AssertTrue(message = "End time must be after start time")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) return true;
        return endTime.isAfter(startTime);
    }
}
