package cz.fit.cvut.seatlock.dto;

import cz.fit.cvut.seatlock.domain.enums.TicketStatus;

public record SeatMapTicket(
        TicketStatus status,
        int seat,
        int row
) {
}
