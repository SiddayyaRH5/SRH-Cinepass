package com.srh.cinepass.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A bookable showtime for a movie at a theatre on a date, including the seat
 * availability the booking UI needs. Never exposes the JPA entity.
 */
public record ShowTimeResponse(
        Long id,
        LocalDate showDate,
        LocalTime showTime,
        Double ticketPrice,
        int totalSeats,
        int availableSeats) {
}
