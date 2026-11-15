package com.srh.cinepass;

import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for H3: a genuine captured payment must never be
 * silently downgraded to CANCELLED/FAILED because a cancellation raced the
 * confirmation.
 */
class BookingConcurrencyTests extends TicketTestSupport {

    @Autowired
    private BookingService bookingService;

    @Test
    void cancellationRequestedAfterPaymentCaptureKeepsTheBookingConfirmedAndTicketValid() {
        Booking booking = pendingBookingWithOrder("order_cancel_after_capture_1");

        bookingService.confirmPaidBooking(
                "order_cancel_after_capture_1",
                "pay_cancel_after_capture_1");

        Ticket issued = ticketRepository.findByBookingId(booking.getId()).orElseThrow();

        Booking afterCancellation = bookingService.cancelPendingBooking(booking.getId());

        assertThat(afterCancellation.getStatus()).isEqualTo("CONFIRMED");
        assertThat(afterCancellation.getPaymentStatus()).isEqualTo("PAID");
        assertThat(afterCancellation.getRazorpayPaymentId())
                .isEqualTo("pay_cancel_after_capture_1");
        assertThat(afterCancellation.getReservationExpiresAt()).isNull();

        Ticket reloaded = ticketRepository.findById(issued.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.BOOKED);
        assertThat(reloaded.getCancelledAt()).isNull();
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void concurrentCancellationAndCapturedPaymentAlwaysEndsConfirmedAndPaid() throws Exception {
        Booking booking = pendingBookingWithOrder("order_cancel_race_1");
        Long bookingId = booking.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Booking> confirmation = () -> {
                start.await(10, TimeUnit.SECONDS);
                return bookingService.confirmPaidBooking(
                        "order_cancel_race_1",
                        "pay_cancel_race_1");
            };

            Callable<Booking> cancellation = () -> {
                start.await(10, TimeUnit.SECONDS);
                return bookingService.cancelPendingBooking(bookingId);
            };

            Future<Booking> confirmationResult = executor.submit(confirmation);
            Future<Booking> cancellationResult = executor.submit(cancellation);

            start.countDown();

            confirmationResult.get(30, TimeUnit.SECONDS);
            cancellationResult.get(30, TimeUnit.SECONDS);

        } finally {
            executor.shutdownNow();
        }

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();

        assertThat(finalBooking.getStatus()).isEqualTo("CONFIRMED");
        assertThat(finalBooking.getPaymentStatus()).isEqualTo("PAID");
        assertThat(finalBooking.getRazorpayPaymentId()).isEqualTo("pay_cancel_race_1");

        Ticket ticket = ticketRepository.findByBookingId(bookingId).orElseThrow();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.BOOKED);
        assertThat(ticket.getCancelledAt()).isNull();
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    private Booking pendingBookingWithOrder(String orderId) {
        User customer = createUser("Concurrency Customer " + orderId, "USER");
        Movie movie = createMovie("Concurrency Movie " + orderId);
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking booking = createBooking(customer, show, "PENDING", "PENDING");
        booking.setRazorpayOrderId(orderId);
        bookingRepository.saveAndFlush(booking);

        return reserveSeatsForPayment(booking);
    }
}
