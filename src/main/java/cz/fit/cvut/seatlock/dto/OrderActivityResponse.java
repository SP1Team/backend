package cz.fit.cvut.seatlock.dto;

import cz.fit.cvut.seatlock.domain.Hall;

public record OrderActivityResponse(
        String name,
        String date,
        String startTime,
        String endTime,
        String description,
        Hall hall
) {}
