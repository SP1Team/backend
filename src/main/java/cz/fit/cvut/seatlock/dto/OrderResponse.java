package cz.fit.cvut.seatlock.dto;

import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        Double totalPrice,
        String status,
        String createdAt,
        List<OrderTicketResponse> tickets,
        OrderActivityResponse activity
) {}
