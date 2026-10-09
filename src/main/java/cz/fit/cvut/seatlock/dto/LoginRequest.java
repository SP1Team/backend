package cz.fit.cvut.seatlock.dto;

public record LoginRequest(
        String email,
        String password
) {
}
