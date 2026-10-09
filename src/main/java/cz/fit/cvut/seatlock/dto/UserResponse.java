package cz.fit.cvut.seatlock.dto;

public record UserResponse(
        String name,
        String surname,
        String email,
        String role,
        String phone
) {
}
