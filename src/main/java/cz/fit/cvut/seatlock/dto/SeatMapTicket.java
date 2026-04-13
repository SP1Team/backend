package cz.fit.cvut.seatlock.dto;

import cz.fit.cvut.seatlock.domain.enums.TicketStatus;
import java.util.UUID;

public record SeatMapTicket(
        UUID id,
        TicketStatus status,
        int seat,
        int row
) {
}
