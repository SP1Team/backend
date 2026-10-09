package cz.fit.cvut.seatlock.dto;

public record UpdateUserRequest(
        String name,
        String surname,
        String email,
        String password
) {
}
