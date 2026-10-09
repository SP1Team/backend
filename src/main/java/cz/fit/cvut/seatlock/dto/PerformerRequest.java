package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.Pattern;

public record PerformerRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Performer name must not be blank")
        String name,
        String description
) {
}
