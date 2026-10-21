package cz.fit.cvut.seatlock.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @NotBlank(message = "Name must not be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Surname must not be blank")
        @Size(max = 255, message = "Surname must not exceed 255 characters")
        String surname,

        @NotBlank(message = "Country code must not be blank")
        @Pattern(regexp = "^\\+?[0-9]{1,4}$", message = "Invalid country code format")
        String countryCode,

        @NotBlank(message = "Phone number must not be blank")
        @Pattern(regexp = "^[0-9]{4,15}$", message = "Invalid phone number format")
        String phoneNumber
) {
}
