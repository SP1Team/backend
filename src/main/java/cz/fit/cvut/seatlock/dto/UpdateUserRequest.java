package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Name must not be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Pattern(regexp = "(?s).*\\S.*", message = "Surname must not be blank")
        @Size(max = 255, message = "Surname must not exceed 255 characters")
        String surname,

        @Pattern(regexp = "(?s).*\\S.*", message = "Email must not be blank")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password
) {
}
