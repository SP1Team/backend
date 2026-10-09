package cz.fit.cvut.seatlock.dto;

import java.util.UUID;

public record OrderTicketResponse(
        UUID id,
        int seatNumber,
        int rowNumber
) {}
