package com.srh.cinepass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Staff verification response. It intentionally contains no email address,
 * phone number, database identifier or verification token.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketVerificationResponse(
        String result,
        String message,
        String ticketReference,
        String movieTitle,
        String theatreName,
        LocalDate showDate,
        LocalTime showTime,
        List<String> seatNumbers,
        Integer quantity,
        String customerName,
        LocalDateTime verifiedAt,
        String verifiedBy,
        String bookingStatus
) {
}
