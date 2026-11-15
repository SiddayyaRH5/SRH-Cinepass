package com.srh.cinepass.dto;

import com.srh.cinepass.entity.Booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Administrator view of a captured payment that could not be confirmed
 * automatically. It deliberately contains no card data, no Razorpay secret and
 * no ticket token; only the information needed to resolve the booking.
 */
public record PaymentRecoveryResponse(
        Long bookingId,
        String userId,
        String userEmail,
        Long showId,
        String movieTitle,
        String theatreName,
        LocalDate showDate,
        LocalTime showTime,
        List<String> seatNumbers,
        Integer numberOfSeats,
        Double totalAmount,
        String bookingStatus,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        String paymentRecoveryStatus,
        String paymentRecoveryReason,
        LocalDateTime paymentRecoveryAt,
        LocalDateTime bookingDate
) {

    public static PaymentRecoveryResponse from(Booking booking) {

        if (booking == null) {
            return null;
        }

        return new PaymentRecoveryResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getUserEmail(),
                booking.getShow() != null ? booking.getShow().getId() : null,
                booking.getShow() != null && booking.getShow().getMovie() != null
                        ? booking.getShow().getMovie().getTitle()
                        : null,
                booking.getShow() != null && booking.getShow().getTheatre() != null
                        ? booking.getShow().getTheatre().getName()
                        : null,
                booking.getShow() != null ? booking.getShow().getShowDate() : null,
                booking.getShow() != null ? booking.getShow().getShowTime() : null,
                booking.getSeatNumbers() == null
                        ? List.of()
                        : List.copyOf(booking.getSeatNumbers()),
                booking.getNumberOfSeats(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getPaymentStatus(),
                booking.getRazorpayOrderId(),
                booking.getRazorpayPaymentId(),
                booking.getPaymentRecoveryStatus(),
                booking.getPaymentRecoveryReason(),
                booking.getPaymentRecoveryAt(),
                booking.getBookingDate());
    }
}
