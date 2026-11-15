package com.srh.cinepass;

import com.srh.cinepass.dto.BookingRequest;
import com.srh.cinepass.dto.TicketVerificationResponse;
import com.srh.cinepass.dto.VerificationResult;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.service.TicketVerificationService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that business time is always IST even when the JVM itself runs in UTC,
 * which is the Render default. The clock is moved to exact IST wall-clock
 * values; the JVM default is forced to UTC for every test in this class.
 */
class TicketTimeZoneTests extends TicketTestSupport {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TicketVerificationService verificationService;

    private TimeZone originalDefault;

    @BeforeEach
    void forceUtcJvm() {
        originalDefault = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreJvmZone() {
        TimeZone.setDefault(originalDefault);
    }

    @Test
    void sixPmIstShowAdmitsAValidTicketAtSixOhFivePmIstWhileTheJvmRunsInUtc() {
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");

        LocalDateTime showStart = LocalDateTime.of(2026, 11, 15, 18, 0);
        testClock.set(LocalDateTime.of(2026, 11, 15, 18, 5));

        User customer = createUser("Ist Customer", "USER");
        User staff = createUser("Ist Staff", "STAFF");
        Movie movie = createMovie("Ist Movie");
        Show show = createShow(movie, showStart);
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        TicketVerificationResponse response = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(response.result()).isEqualTo(VerificationResult.VALID.name());
        assertThat(response.message()).isEqualTo("Ticket Verified Successfully");
    }

    @Test
    void admissionOpensExactlyTwoHoursBeforeTheIstShow() {
        LocalDateTime showStart = LocalDateTime.of(2026, 11, 15, 18, 0);

        assertAdmissionResult(showStart, showStart.minusMinutes(120),
                VerificationResult.VALID);
        assertAdmissionResult(showStart, showStart.minusMinutes(120).minusSeconds(1),
                VerificationResult.NOT_YET_VALID);
    }

    @Test
    void admissionClosesExactlyThirtyMinutesAfterTheIstShow() {
        LocalDateTime showStart = LocalDateTime.of(2026, 11, 15, 18, 0);

        assertAdmissionResult(showStart, showStart.plusMinutes(30),
                VerificationResult.VALID);
        assertAdmissionResult(showStart, showStart.plusMinutes(30).plusSeconds(1),
                VerificationResult.EXPIRED);
    }

    @Test
    void ticketForAFutureIstShowIsNotYetValid() {
        LocalDateTime showStart = LocalDateTime.of(2026, 11, 15, 18, 0);
        testClock.set(LocalDateTime.of(2026, 11, 15, 12, 0));

        User customer = createUser("Early Ist Customer", "USER");
        User staff = createUser("Early Ist Staff", "STAFF");
        Movie movie = createMovie("Early Ist Movie");
        Show show = createShow(movie, showStart);
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        TicketVerificationResponse response = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(response.result()).isEqualTo(VerificationResult.NOT_YET_VALID.name());
    }

    @Test
    void capturedPaymentIsRecoveredWhenTheReservationExpiredButSeatsAreStillFree() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 17, 0);
        testClock.set(now);

        User customer = createUser("Recovery Customer", "USER");
        Movie movie = createMovie("Recovery Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Booking booking = createBooking(customer, show, "PENDING", "PENDING");
        booking.setRazorpayOrderId("order_recovery_1");
        bookingRepository.saveAndFlush(booking);
        reserveSeatsForPayment(booking);

        // Move past the ten-minute reservation window.
        testClock.advance(Duration.ofMinutes(11));

        Booking confirmed = bookingService.confirmPaidBooking(
                "order_recovery_1",
                "pay_recovery_1");

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(confirmed.getPaymentRecoveryStatus())
                .isEqualTo(BookingService.RECOVERY_RECOVERED);
        assertThat(ticketRepository.findByBookingId(booking.getId())).isPresent();

        for (Seat seat : seatRepository.findByShowIdOrderByIdAsc(show.getId())) {
            assertThat(seat.isPermanentlyBooked()).isTrue();
        }
    }

    @Test
    void capturedPaymentIsParkedForManualReviewWhenTheSeatsWereResold() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 17, 0);
        testClock.set(now);

        User customer = createUser("Resold Customer", "USER");
        Movie movie = createMovie("Resold Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Booking booking = createBooking(customer, show, "PENDING", "PENDING");
        booking.setRazorpayOrderId("order_resold_1");
        bookingRepository.saveAndFlush(booking);
        reserveSeatsForPayment(booking);

        testClock.advance(Duration.ofMinutes(11));

        // Another customer bought the first seat while this reservation was
        // expiring. It must never be stolen back.
        Seat resold = seatRepository.findByShowIdOrderByIdAsc(show.getId()).stream()
                .filter(seat -> "S1".equals(seat.getSeatNumber()))
                .findFirst()
                .orElseThrow();

        resold.setBooked(true);
        resold.setReservedByBookingId(999_999L);
        resold.setReservedUntil(LocalDateTime.now(testClock).plusMinutes(10));
        seatRepository.saveAndFlush(resold);

        Booking reviewed = bookingService.confirmPaidBooking(
                "order_resold_1",
                "pay_resold_1");

        assertThat(reviewed.getStatus()).isEqualTo("PAYMENT_REVIEW");
        assertThat(reviewed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(reviewed.getPaymentRecoveryStatus())
                .isEqualTo(BookingService.RECOVERY_MANUAL_REVIEW);
        assertThat(reviewed.getPaymentRecoveryReason()).contains("S1");
        assertThat(reviewed.getRazorpayPaymentId()).isEqualTo("pay_resold_1");
        assertThat(ticketRepository.findByBookingId(booking.getId())).isEmpty();

        Seat reloaded = seatRepository.findById(resold.getId()).orElseThrow();
        assertThat(reloaded.isPermanentlyBooked()).isTrue();
        assertThat(reloaded.getReservedByBookingId()).isEqualTo(999_999L);
    }

    @Test
    void attachingAnOrderAfterTheReservationWindowFails() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User customer = createUser("Expiry Customer", "USER");
        Movie movie = createMovie("Expiry Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Seat first = createSeat(show, "A1");
        Seat second = createSeat(show, "A2");

        BookingRequest request = new BookingRequest();
        request.setShowId(show.getId());
        request.setSeatIds(List.of(first.getId(), second.getId()));
        request.setUserId(String.valueOf(customer.getId()));
        request.setUserEmail(customer.getEmail());

        Booking pending = bookingService.createPendingBooking(request);

        assertThat(pending.getReservationExpiresAt())
                .isEqualTo(LocalDateTime.of(2026, 11, 15, 17, 10));

        testClock.advance(Duration.ofMinutes(11));

        assertThatThrownBy(() -> bookingService.attachRazorpayOrder(
                pending.getId(),
                "order_expired_1"))
                .hasMessageContaining("expired");
    }

    @Test
    void temporarySeatReservationExpiresAtTheIstBoundary() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 11, 15, 18, 10);

        Seat seat = new Seat();
        seat.setSeatNumber("T1");
        seat.setReservedByBookingId(123L);
        seat.setReservedUntil(expiresAt);

        assertThat(seat.isBookedAt(expiresAt.minusSeconds(1))).isTrue();
        assertThat(seat.isBookedAt(expiresAt)).isFalse();
        assertThat(seat.isBookedAt(expiresAt.plusSeconds(1))).isFalse();

        seat.setBooked(true);

        assertThat(seat.isBookedAt(expiresAt.minusHours(1))).isTrue();
        assertThat(seat.isBookedAt(expiresAt.plusHours(1))).isTrue();
    }

    @Test
    void seatReservationCreatedFromTheInjectedIstClockExpiresTenMinutesLater() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User customer = createUser("Seat Expiry Customer", "USER");
        Movie movie = createMovie("Seat Expiry Movie");
        Show show = createShow(movie, LocalDateTime.of(2026, 11, 15, 20, 0));

        Seat seat = createSeat(show, "R1");

        BookingRequest request = new BookingRequest();
        request.setShowId(show.getId());
        request.setSeatIds(List.of(seat.getId()));
        request.setUserId(String.valueOf(customer.getId()));
        request.setUserEmail(customer.getEmail());

        bookingService.createPendingBooking(request);

        Seat reserved = seatRepository.findById(seat.getId()).orElseThrow();

        assertThat(reserved.getReservedUntil())
                .isEqualTo(LocalDateTime.of(2026, 11, 15, 17, 10));

        // The injected IST clock decides availability, not the JVM default.
        assertThat(reserved.isBookedAt(LocalDateTime.of(2026, 11, 15, 17, 9, 59))).isTrue();
        assertThat(reserved.isBookedAt(LocalDateTime.of(2026, 11, 15, 17, 10))).isFalse();
    }

    private void assertAdmissionResult(
            LocalDateTime showStart,
            LocalDateTime now,
            VerificationResult expected) {

        testClock.set(now);

        User customer = createUser("Window Customer " + expected.name(), "USER");
        User staff = createUser("Window Staff " + expected.name(), "STAFF");
        Movie movie = createMovie("Window Movie " + expected.name());
        Show show = createShow(movie, showStart);
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        TicketVerificationResponse response = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(response.result()).isEqualTo(expected.name());
    }

    private Seat createSeat(Show show, String seatNumber) {
        Seat seat = new Seat();
        seat.setSeatNumber(seatNumber);
        seat.setShow(show);
        return seatRepository.saveAndFlush(seat);
    }
}
