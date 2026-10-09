package cz.fit.cvut.seatlock.dto;

import java.util.List;

public record HallRowResponse(
        int number,
        List<HallSeatResponse> seats
) {

}
