package com.srh.cinepass;

import com.srh.cinepass.dto.BookingRequest;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Booking regression around the show lifecycle. A show that is expired,
 * cancelled, or whose sales window has closed must reject new orders, while a
 * normal show keeps the existing pending-booking + reservation behaviour.
 */
class ShowBookingWindowTests extends TicketTestSupport {

    @Autowired
    private BookingService bookingService;

    @Test
    void aNormalShowStillAcceptsAPendingBookingAndReservesTheSeat() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User customer = createUser("Window Customer", "USER");
        Movie movie = createMovie("Window Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Seat seat = createSeat(show, "S1");

        Booking booking = bookingService.createPendingBooking(
                requestFor(show, seat, customer));

        assertThat(booking.getStatus()).isEqualTo("PENDING");
        assertThat(booking.getPaymentStatus()).isEqualTo("PENDING");

        Seat reserved = seatRepository.findById(seat.getId()).orElseThrow();

        assertThat(reserved.getReservedUntil()).isNotNull();
        assertThat(reserved.getReservedByBookingId()).isEqualTo(booking.getId());
    }

    @Test
    void anExpiredShowCannotReceiveANewBooking() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User customer = createUser("Expired Show Customer", "USER");
        Movie movie = createMovie("Expired Show Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        show.setStatus(ShowStatus.EXPIRED);
        showRepository.saveAndFlush(show);

        Seat seat = createSeat(show, "S1");

        assertThatThrownBy(() -> bookingService.createPendingBooking(
                requestFor(show, seat, customer)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer available");

        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void aCancelledShowCannotReceiveANewBooking() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User customer = createUser("Cancelled Show Customer", "USER");
        Movie movie = createMovie("Cancelled Show Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        show.setStatus(ShowStatus.CANCELLED);
        showRepository.saveAndFlush(show);

        Seat seat = createSeat(show, "S1");

        assertThatThrownBy(() -> bookingService.createPendingBooking(
                requestFor(show, seat, customer)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void salesCloseFifteenMinutesBeforeTheShowStarts() {

        // Show at 20:00, sales close at 19:45.
        testClock.set(LocalDateTime.of(2026, 11, 15, 19, 46));

        User customer = createUser("Late Customer", "USER");
        Movie movie = createMovie("Late Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Seat seat = createSeat(show, "S1");

        assertThatThrownBy(() -> bookingService.createPendingBooking(
                requestFor(show, seat, customer)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void aReservedSeatCannotBeBookedTwice() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User firstCustomer = createUser("First Customer", "USER");
        User secondCustomer = createUser("Second Customer", "USER");
        Movie movie = createMovie("Double Booking Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Seat seat = createSeat(show, "S1");

        bookingService.createPendingBooking(requestFor(show, seat, firstCustomer));

        assertThatThrownBy(() -> bookingService.createPendingBooking(
                requestFor(show, seat, secondCustomer)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already booked or reserved");
    }

    private Seat createSeat(Show show, String seatNumber) {

        Seat seat = new Seat();
        seat.setSeatNumber(seatNumber);
        seat.setShow(show);

        return seatRepository.saveAndFlush(seat);
    }

    private BookingRequest requestFor(
            Show show,
            Seat seat,
            User user) {

        BookingRequest request = new BookingRequest();
        request.setShowId(show.getId());
        request.setSeatIds(List.of(seat.getId()));
        request.setUserId(String.valueOf(user.getId()));
        request.setUserEmail(user.getEmail());

        return request;
    }
}
