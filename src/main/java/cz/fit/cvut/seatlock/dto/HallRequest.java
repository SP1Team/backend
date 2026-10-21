package cz.fit.cvut.seatlock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HallRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Hall name must not be blank")
        @Size(max = 255, message = "Hall name must not exceed 255 characters")
        String name,

        @Pattern(regexp = "(?s).*\\S.*", message = "Hall address must not be blank")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Pattern(regexp = "(?s).*\\S.*", message = "City must not be blank")
        @Size(max = 255, message = "City must not exceed 255 characters")
        String city,

        List<@Valid RowConfig> seats
) {
}
