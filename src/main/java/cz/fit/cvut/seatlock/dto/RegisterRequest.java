package cz.fit.cvut.seatlock.dto;

public record RegisterRequest(
        String email,
        String password,
        String name,
        String surname,
        String countryCode,
        String phoneNumber
) {
}
