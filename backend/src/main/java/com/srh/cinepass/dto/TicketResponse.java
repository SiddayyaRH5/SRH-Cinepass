package com.srh.cinepass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Customer-facing ticket projection. The verification token is deliberately
 * absent: customers receive only the QR PNG through an authenticated endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketResponse(
        Long ticketId,
        Long bookingId,
        String ticketReference,
        String status,
        String bookingStatus,
        String movieTitle,
        String moviePosterUrl,
        String customerName,
        String theatreName,
        String theatreCity,
        String screenNumber,
        LocalDate showDate,
        LocalTime showTime,
        List<String> seatNumbers,
        Integer quantity,
        LocalDateTime bookingTimestamp,
        LocalDateTime issuedAt,
        LocalDateTime verifiedAt
) {
}
