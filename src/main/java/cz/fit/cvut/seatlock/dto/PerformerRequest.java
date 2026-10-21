package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerformerRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Performer name must not be blank")
        @Size(max = 255, message = "Performer name must not exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {
}
