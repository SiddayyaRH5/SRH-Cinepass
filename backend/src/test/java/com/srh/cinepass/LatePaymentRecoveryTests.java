package com.srh.cinepass;

import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 9A: deterministic coverage for a captured payment that arrives after a
 * pending booking has already been fully cancelled and its seats released.
 *
 * <p>These tests use the real {@link BookingService} transaction paths, the
 * real pessimistic seat locking and the real ticket-issuance service. Nothing
 * about locking, reservation or ticket issuance is mocked.</p>
 */
class LatePaymentRecoveryTests extends TicketTestSupport {

    @Autowired
    private BookingService bookingService;

    /**
     * Cancellation finishes first (booking CANCELLED, seats released), then the
     * captured payment arrives. The original seats are still free, so the
     * booking must be recovered and exactly one ticket issued.
     */
    @Test
    void completedCancellationThenCapturedPaymentRecoversTheBookingAndIssuesOneTicket() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User customer = createUser("Late Capture Customer", "USER");
        Movie movie = createMovie("Late Capture Movie");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking booking = createBooking(customer, show, "PENDING", "PENDING");
        booking.setRazorpayOrderId("order_late_capture_1");
        bookingRepository.saveAndFlush(booking);
        booking = reserveSeatsForPayment(booking);

        Long bookingId = booking.getId();
        String orderId = booking.getRazorpayOrderId();
        Double totalAmount = booking.getTotalAmount();

        // STEP 1: cancellation completes before the capture is reported.
        Booking cancelled = bookingService.cancelPendingBooking(bookingId);

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(cancelled.getRazorpayOrderId()).isEqualTo(orderId);
        assertThat(cancelled.getTotalAmount()).isEqualTo(totalAmount);
        assertThat(cancelled.getReservationExpiresAt()).isNull();

        for (Seat seat : seatRepository.findByShowIdOrderByIdAsc(show.getId())) {
            assertThat(seat.isPermanentlyBooked()).isFalse();
            assertThat(seat.getReservedByBookingId()).isNull();
            assertThat(seat.getReservedUntil()).isNull();
        }

        // STEP 2: Razorpay reports the capture for the already-cancelled order.
        Booking confirmed = bookingService.confirmPaidBooking(
                orderId,
                "pay_late_capture_1");

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(confirmed.getPaymentRecoveryStatus())
                .isEqualTo(BookingService.RECOVERY_RECOVERED);
        assertThat(confirmed.getRazorpayPaymentId()).isEqualTo("pay_late_capture_1");
        assertThat(confirmed.getRazorpayOrderId()).isEqualTo(orderId);
        assertThat(confirmed.getTotalAmount()).isEqualTo(totalAmount);

        for (Seat seat : seatRepository.findByShowIdOrderByIdAsc(show.getId())) {
            assertThat(seat.isPermanentlyBooked()).isTrue();
            assertThat(seat.getReservedByBookingId()).isNull();
            assertThat(seat.getReservedUntil()).isNull();
        }

        List<Ticket> tickets = ticketRepository.findAll();

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getBooking().getId()).isEqualTo(bookingId);
        assertThat(tickets.get(0).getStatus()).isEqualTo(TicketStatus.BOOKED);
        assertThat(tickets.get(0).getCancelledAt()).isNull();

        // STEP 3: a retried confirmation must stay idempotent.
        Booking reconfirmed = bookingService.confirmPaidBooking(
                orderId,
                "pay_late_capture_1");

        assertThat(reconfirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(reconfirmed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(reconfirmed.getRazorpayPaymentId()).isEqualTo("pay_late_capture_1");
        assertThat(ticketRepository.count()).isEqualTo(1);
        assertThat(ticketRepository.findByBookingId(bookingId)).isPresent();
    }

    /**
     * Cancellation finishes, the released seats are then reserved by a second
     * booking, and only afterwards is the first booking's capture reported.
     * The first booking must be parked for manual review and must never steal
     * the second booking's reservation or receive a ticket.
     */
    @Test
    void completedCancellationThenCapturedPaymentWhenSeatsWereResoldParksForManualReview() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User firstCustomer = createUser("Resold First Customer", "USER");
        User secondCustomer = createUser("Resold Second Customer", "USER");
        Movie movie = createMovie("Resold Late Capture Movie");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking first = createBooking(firstCustomer, show, "PENDING", "PENDING");
        first.setRazorpayOrderId("order_late_resold_1");
        bookingRepository.saveAndFlush(first);
        first = reserveSeatsForPayment(first);

        Long firstBookingId = first.getId();
        String firstOrderId = first.getRazorpayOrderId();

        // STEP 1: the first booking is fully cancelled and its seats released.
        Booking firstCancelled = bookingService.cancelPendingBooking(firstBookingId);

        assertThat(firstCancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(firstCancelled.getPaymentStatus()).isEqualTo("FAILED");

        // STEP 2: a second booking takes over the released seats.
        Booking second = createBooking(secondCustomer, show, "PENDING", "PENDING");
        second = reserveExistingSeatsFor(second);

        Long secondBookingId = second.getId();
        LocalDateTime secondReservationExpiry = second.getReservationExpiresAt();

        assertThat(secondReservationExpiry).isNotNull();

        // STEP 3: the first booking's capture arrives after the seats were resold.
        Booking reviewed = bookingService.confirmPaidBooking(
                firstOrderId,
                "pay_late_resold_1");

        assertThat(reviewed.getStatus()).isEqualTo("PAYMENT_REVIEW");
        assertThat(reviewed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(reviewed.getPaymentRecoveryStatus())
                .isEqualTo(BookingService.RECOVERY_MANUAL_REVIEW);
        assertThat(reviewed.getRazorpayPaymentId()).isEqualTo("pay_late_resold_1");
        assertThat(reviewed.getPaymentRecoveryReason()).contains("no longer available");

        assertThat(ticketRepository.findByBookingId(firstBookingId)).isEmpty();
        assertThat(ticketRepository.count()).isZero();

        // The second booking's reservation is untouched: no seat was stolen
        // and the seats were not permanently booked for the first booking.
        for (Seat seat : seatRepository.findByShowIdOrderByIdAsc(show.getId())) {
            assertThat(seat.isPermanentlyBooked()).isFalse();
            assertThat(seat.getReservedByBookingId()).isEqualTo(secondBookingId);
            assertThat(seat.getReservedUntil()).isEqualTo(secondReservationExpiry);
        }

        Booking reloadedSecond = bookingRepository.findById(secondBookingId).orElseThrow();

        assertThat(reloadedSecond.getStatus()).isEqualTo("PENDING");
        assertThat(reloadedSecond.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(reloadedSecond.getReservationExpiresAt()).isEqualTo(secondReservationExpiry);
    }

    /**
     * Reserves the already-existing seat rows for a new booking, mirroring what
     * the real checkout flow does after a previous reservation is released.
     */
    private Booking reserveExistingSeatsFor(Booking booking) {
        LocalDateTime expiresAt = LocalDateTime.now(testClock).plusMinutes(10);

        booking.setReservationExpiresAt(expiresAt);
        Booking saved = bookingRepository.saveAndFlush(booking);

        List<Seat> seats = seatRepository.findByShowIdOrderByIdAsc(saved.getShow().getId());

        for (Seat seat : seats) {
            if (saved.getSeatNumbers().contains(seat.getSeatNumber())) {
                seat.setReservedByBookingId(saved.getId());
                seat.setReservedUntil(expiresAt);
            }
        }

        seatRepository.saveAllAndFlush(seats);

        return saved;
    }
}
