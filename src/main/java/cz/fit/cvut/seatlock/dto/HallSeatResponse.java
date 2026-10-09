package cz.fit.cvut.seatlock.dto;

import java.util.UUID;

public record HallSeatResponse(
        UUID id,
        int seatNumber,
        int rowNumber
) {

}
