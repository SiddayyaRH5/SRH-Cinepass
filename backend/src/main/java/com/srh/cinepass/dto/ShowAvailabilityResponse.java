package com.srh.cinepass.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Live availability for a single show. The frontend calls this immediately
 * before creating an order so a stale page can never book a closed show.
 */
public record ShowAvailabilityResponse(
        Long showId,
        LocalDate showDate,
        LocalTime showTime,
        Double ticketPrice,
        int totalSeats,
        int availableSeats,
        boolean bookable,
        String status) {
}
