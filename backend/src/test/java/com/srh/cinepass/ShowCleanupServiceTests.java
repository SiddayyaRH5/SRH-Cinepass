package com.srh.cinepass;

import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.ShowCleanupService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Show lifecycle cleanup: only shows past the grace period are marked EXPIRED,
 * and no booking, ticket or payment history is touched.
 */
class ShowCleanupServiceTests extends TicketTestSupport {

    @Autowired
    private ShowCleanupService cleanupService;

    @Test
    void marksOnlyShowsPastTheGracePeriodExpiredAndPreservesHistory() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 12, 0));

        User customer = createUser("Cleanup Customer", "USER");
        Movie movie = createMovie("Cleanup Movie");

        // Started 60 minutes ago -> beyond the 45 minute grace period.
        Show finished = createShow(movie, LocalDateTime.of(2026, 11, 15, 11, 0));

        // Started 30 minutes ago -> still inside the 45 minute grace period.
        Show insideGrace = createShow(movie, LocalDateTime.of(2026, 11, 15, 11, 30));

        // Later today -> untouched.
        Show upcoming = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Booking booking = createConfirmedBooking(customer, finished);
        Ticket ticket = issueTicket(booking);

        int updated = cleanupService.expireFinishedShows();

        assertThat(updated).isEqualTo(1);

        assertThat(showRepository.findById(finished.getId()).orElseThrow().getStatus())
                .isEqualTo(ShowStatus.EXPIRED);
        assertThat(showRepository.findById(insideGrace.getId()).orElseThrow().getStatus())
                .isNull();
        assertThat(showRepository.findById(upcoming.getId()).orElseThrow().getStatus())
                .isNull();

        // Booking, payment status and issued ticket are untouched.
        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(reloadedBooking.getStatus()).isEqualTo("CONFIRMED");
        assertThat(reloadedBooking.getPaymentStatus()).isEqualTo("PAID");

        Ticket reloadedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertThat(reloadedTicket.getStatus()).isEqualTo(TicketStatus.BOOKED);

        // Idempotent: a second run changes nothing.
        assertThat(cleanupService.expireFinishedShows()).isZero();
        assertThat(showRepository.findById(finished.getId()).orElseThrow().getStatus())
                .isEqualTo(ShowStatus.EXPIRED);
    }

    @Test
    void usesTheConfiguredGracePeriod() {
        assertThat(cleanupService.getMinutesAfterStart()).isEqualTo(45);
    }
}
