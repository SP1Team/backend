package cz.fit.cvut.seatlock.dto;

import java.util.List;

public record HallSeatMapResponse(
        List<HallRowResponse> rows
) {

}
