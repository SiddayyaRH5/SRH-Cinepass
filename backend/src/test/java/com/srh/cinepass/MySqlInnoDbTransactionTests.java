package com.srh.cinepass;

import com.srh.cinepass.dto.BookingRequest;
import com.srh.cinepass.dto.WebhookResult;
import com.srh.cinepass.dto.WebhookResult.WebhookOutcome;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.service.RazorpayWebhookEventStore;
import com.srh.cinepass.service.RazorpayWebhookService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 9B: real MySQL 8 / InnoDB transaction and locking tests.
 *
 * <p><strong>This is NOT migration validation.</strong> Hibernate's
 * {@code create-drop} schema generation is used only as a temporary
 * disposable baseline so that the real service transactions and pessimistic
 * locks can be exercised against MySQL instead of H2. Migrations 001 and 002
 * remain additive and incomplete and are reserved for Phase 9C.</p>
 *
 * <p>The class is skipped (not silently redirected to H2) when Docker is not
 * available. When it runs, the datasource points at the container's mapped
 * JDBC port and uses throw-away credentials only.</p>
 */
@ActiveProfiles("mysql-it")
@Testcontainers(disabledWithoutDocker = true)
class MySqlInnoDbTransactionTests extends TicketTestSupport {

    private static final String MYSQL_IMAGE = "mysql:8.0.36";
    private static final String WEBHOOK_SECRET = "test_webhook_secret";
    private static final long AMOUNT_IN_PAISE = 50_000L;

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer(
            DockerImageName.parse(MYSQL_IMAGE))
            .withDatabaseName("cinepass_it")
            .withUsername("cinepass")
            .withPassword("cinepass");

    @DynamicPropertySource
    static void mysqlDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RazorpayWebhookService webhookService;

    @Autowired
    private RazorpayWebhookEventStore eventStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ------------------------------------------------------------------
    // Proof that the container is real MySQL and the default engine is InnoDB
    // ------------------------------------------------------------------

    @Test
    void runsAgainstRealMySqlWithInnoDb() {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        String engine = jdbcTemplate.queryForObject(
                "SELECT @@default_storage_engine", String.class);
        String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);

        assertThat(version).startsWith("8.");
        assertThat(engine).isEqualToIgnoringCase("InnoDB");
        assertThat(database).isEqualTo("cinepass_it");
    }

    // ------------------------------------------------------------------
    // 1. Booking row locking: concurrent cancellation + confirmation
    // ------------------------------------------------------------------

    @Test
    void concurrentCancellationAndCapturedPaymentEndsConfirmedPaidWithOneTicket()
            throws Exception {

        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        Booking booking = pendingBookingWithOrder("order_mysql_race_1");
        Long bookingId = booking.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Booking> confirmation = () -> {
                start.await(10, TimeUnit.SECONDS);
                return bookingService.confirmPaidBooking(
                        "order_mysql_race_1",
                        "pay_mysql_race_1");
            };

            Callable<Booking> cancellation = () -> {
                start.await(10, TimeUnit.SECONDS);
                return bookingService.cancelPendingBooking(bookingId);
            };

            Future<Booking> confirmationResult = executor.submit(confirmation);
            Future<Booking> cancellationResult = executor.submit(cancellation);

            start.countDown();

            confirmationResult.get(60, TimeUnit.SECONDS);
            cancellationResult.get(60, TimeUnit.SECONDS);

        } finally {
            executor.shutdownNow();
        }

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();

        assertThat(finalBooking.getStatus()).isEqualTo("CONFIRMED");
        assertThat(finalBooking.getStatus()).isNotEqualTo("CANCELLED");
        assertThat(finalBooking.getPaymentStatus()).isEqualTo("PAID");
        assertThat(finalBooking.getRazorpayPaymentId()).isEqualTo("pay_mysql_race_1");

        Ticket ticket = ticketRepository.findByBookingId(bookingId).orElseThrow();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.BOOKED);
        assertThat(ticket.getCancelledAt()).isNull();
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // 2. Seat locking: two competing bookings for the same seat
    // ------------------------------------------------------------------

    @Test
    void competingBookingsForTheSameSeatProduceExactlyOneReservation()
            throws Exception {

        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User firstCustomer = createUser("MySQL Seat Race A", "USER");
        User secondCustomer = createUser("MySQL Seat Race B", "USER");
        Movie movie = createMovie("MySQL Seat Race Movie");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Seat seat = new Seat();
        seat.setSeatNumber("S1");
        seat.setShow(show);
        seat = seatRepository.saveAndFlush(seat);

        Long seatId = seat.getId();

        BookingRequest firstRequest = bookingRequest(firstCustomer, show, seatId);
        BookingRequest secondRequest = bookingRequest(secondCustomer, show, seatId);

        AtomicInteger successes = new AtomicInteger();
        AtomicReference<Booking> winner = new AtomicReference<>();
        AtomicReference<Throwable> loser = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Void> firstAttempt = () -> {
                start.await(10, TimeUnit.SECONDS);

                try {
                    winner.set(bookingService.createPendingBooking(firstRequest));
                    successes.incrementAndGet();
                } catch (Throwable failure) {
                    loser.set(failure);
                }

                return null;
            };

            Callable<Void> secondAttempt = () -> {
                start.await(10, TimeUnit.SECONDS);

                try {
                    winner.set(bookingService.createPendingBooking(secondRequest));
                    successes.incrementAndGet();
                } catch (Throwable failure) {
                    loser.set(failure);
                }

                return null;
            };

            Future<Void> firstResult = executor.submit(firstAttempt);
            Future<Void> secondResult = executor.submit(secondAttempt);

            start.countDown();

            firstResult.get(60, TimeUnit.SECONDS);
            secondResult.get(60, TimeUnit.SECONDS);

        } finally {
            executor.shutdownNow();
        }

        assertThat(successes.get()).isEqualTo(1);
        assertThat(winner.get()).isNotNull();
        assertThat(loser.get()).isNotNull();
        assertThat(loser.get()).isInstanceOf(IllegalArgumentException.class);
        assertThat(loser.get().getMessage()).contains("already booked or reserved");

        Seat reloaded = seatRepository.findById(seatId).orElseThrow();

        assertThat(reloaded.isPermanentlyBooked()).isFalse();
        assertThat(reloaded.getReservedByBookingId()).isEqualTo(winner.get().getId());
        assertThat(reloaded.getReservedUntil())
                .isAfter(LocalDateTime.now(testClock));

        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // 3. Webhook event uniqueness and retryable behaviour
    // ------------------------------------------------------------------

    @Test
    void concurrentWebhookEventClaimsAllowOnlyOneWinnerAndLoserGetsRetryableState()
            throws Exception {

        String eventId = "evt_mysql_concurrent_1";

        List<RazorpayWebhookEventStore.Claim> claims = claimConcurrently(eventId);

        long winners = claims.stream()
                .filter(RazorpayWebhookEventStore.Claim::claimed)
                .count();

        assertThat(winners).isEqualTo(1);

        RazorpayWebhookEventStore.Claim loser = claims.stream()
                .filter(claim -> !claim.claimed())
                .findFirst()
                .orElseThrow();

        assertThat(loser.event()).isNotNull();
        assertThat(loser.event().getStatus()).isEqualTo("PROCESSING");

        String payload = "{\"event\":\"payment.captured\",\"payload\":{}}";

        // While the winner is still PROCESSING, a duplicate delivery must be
        // told to retry, never acknowledged with a terminal 200.
        WebhookResult retry = webhookService.handle(
                payload.getBytes(StandardCharsets.UTF_8),
                sign(payload),
                eventId);

        assertThat(retry.status()).isEqualTo(WebhookOutcome.RETRY);

        // Once the winner completes, the duplicate is terminal.
        eventStore.complete(
                eventId,
                WebhookResult.processed("completed", null, eventId),
                null);

        WebhookResult duplicate = webhookService.handle(
                payload.getBytes(StandardCharsets.UTF_8),
                sign(payload),
                eventId);

        assertThat(duplicate.status()).isEqualTo(WebhookOutcome.DUPLICATE);
    }

    @Test
    void completedDuplicateWebhookDeliveryDoesNotIssueASecondTicket()
            throws Exception {

        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        Booking booking = pendingBookingWithOrder("order_mysql_duplicate_1");

        String payload = capturedPaymentPayload(
                "payment.captured",
                "order_mysql_duplicate_1",
                "pay_mysql_duplicate_1",
                AMOUNT_IN_PAISE,
                "INR");

        String signature = sign(payload);
        String eventId = "evt_mysql_duplicate_1";

        WebhookResult first = webhookService.handle(
                payload.getBytes(StandardCharsets.UTF_8),
                signature,
                eventId);

        assertThat(first.status()).isEqualTo(WebhookOutcome.PROCESSED);
        assertThat(ticketRepository.count()).isEqualTo(1);

        WebhookResult second = webhookService.handle(
                payload.getBytes(StandardCharsets.UTF_8),
                signature,
                eventId);

        assertThat(second.status()).isEqualTo(WebhookOutcome.DUPLICATE);
        assertThat(ticketRepository.count()).isEqualTo(1);

        Booking confirmed = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
    }

    // ------------------------------------------------------------------
    // 4. Late payment recovery on MySQL
    // ------------------------------------------------------------------

    @Test
    void cancelledBookingThenCapturedPaymentRecoversFreeSeatsAndIssuesOneTicket() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User customer = createUser("MySQL Late Capture Customer", "USER");
        Movie movie = createMovie("MySQL Late Capture Movie");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking booking = createBooking(customer, show, "PENDING", "PENDING");
        booking.setRazorpayOrderId("order_mysql_late_capture_1");
        bookingRepository.saveAndFlush(booking);
        booking = reserveSeatsForPayment(booking);

        Long bookingId = booking.getId();
        String orderId = booking.getRazorpayOrderId();
        Double totalAmount = booking.getTotalAmount();

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

        Booking confirmed = bookingService.confirmPaidBooking(
                orderId,
                "pay_mysql_late_capture_1");

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(confirmed.getPaymentRecoveryStatus())
                .isEqualTo(BookingService.RECOVERY_RECOVERED);
        assertThat(confirmed.getRazorpayPaymentId())
                .isEqualTo("pay_mysql_late_capture_1");
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

        bookingService.confirmPaidBooking(orderId, "pay_mysql_late_capture_1");

        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void cancelledBookingThenCapturedPaymentWithResoldSeatsParksForManualReview() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));

        User firstCustomer = createUser("MySQL Resold First", "USER");
        User secondCustomer = createUser("MySQL Resold Second", "USER");
        Movie movie = createMovie("MySQL Resold Late Capture Movie");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking first = createBooking(firstCustomer, show, "PENDING", "PENDING");
        first.setRazorpayOrderId("order_mysql_late_resold_1");
        bookingRepository.saveAndFlush(first);
        first = reserveSeatsForPayment(first);

        Long firstBookingId = first.getId();
        String firstOrderId = first.getRazorpayOrderId();

        Booking firstCancelled = bookingService.cancelPendingBooking(firstBookingId);

        assertThat(firstCancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(firstCancelled.getPaymentStatus()).isEqualTo("FAILED");

        Booking second = createBooking(secondCustomer, show, "PENDING", "PENDING");
        second = reserveExistingSeatsFor(second);

        Long secondBookingId = second.getId();
        LocalDateTime secondReservationExpiry = second.getReservationExpiresAt();

        assertThat(secondReservationExpiry).isNotNull();

        Booking reviewed = bookingService.confirmPaidBooking(
                firstOrderId,
                "pay_mysql_late_resold_1");

        assertThat(reviewed.getStatus()).isEqualTo("PAYMENT_REVIEW");
        assertThat(reviewed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(reviewed.getPaymentRecoveryStatus())
                .isEqualTo(BookingService.RECOVERY_MANUAL_REVIEW);
        assertThat(reviewed.getRazorpayPaymentId())
                .isEqualTo("pay_mysql_late_resold_1");
        assertThat(reviewed.getPaymentRecoveryReason()).contains("no longer available");

        assertThat(ticketRepository.findByBookingId(firstBookingId)).isEmpty();
        assertThat(ticketRepository.count()).isZero();

        for (Seat seat : seatRepository.findByShowIdOrderByIdAsc(show.getId())) {
            assertThat(seat.isPermanentlyBooked()).isFalse();
            assertThat(seat.getReservedByBookingId()).isEqualTo(secondBookingId);
            assertThat(seat.getReservedUntil()).isEqualTo(secondReservationExpiry);
        }

        Booking reloadedSecond = bookingRepository.findById(secondBookingId).orElseThrow();

        assertThat(reloadedSecond.getStatus()).isEqualTo("PENDING");
        assertThat(reloadedSecond.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(reloadedSecond.getReservationExpiresAt())
                .isEqualTo(secondReservationExpiry);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<RazorpayWebhookEventStore.Claim> claimConcurrently(String eventId)
            throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<RazorpayWebhookEventStore.Claim> claim = () -> {
                start.await(10, TimeUnit.SECONDS);
                return eventStore.claim(eventId, "payment.captured");
            };

            Future<RazorpayWebhookEventStore.Claim> first = executor.submit(claim);
            Future<RazorpayWebhookEventStore.Claim> second = executor.submit(claim);

            start.countDown();

            return List.of(
                    first.get(60, TimeUnit.SECONDS),
                    second.get(60, TimeUnit.SECONDS));

        } finally {
            executor.shutdownNow();
        }
    }

    private Booking pendingBookingWithOrder(String orderId) {
        User customer = createUser("MySQL Webhook Customer " + orderId, "USER");
        Movie movie = createMovie("MySQL Webhook Movie " + orderId);
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking booking = createBooking(customer, show, "PENDING", "PENDING");
        booking.setRazorpayOrderId(orderId);
        bookingRepository.saveAndFlush(booking);

        return reserveSeatsForPayment(booking);
    }

    private BookingRequest bookingRequest(User user, Show show, Long seatId) {
        BookingRequest request = new BookingRequest();
        request.setUserId(String.valueOf(user.getId()));
        request.setUserEmail(user.getEmail());
        request.setShowId(show.getId());
        request.setSeatIds(List.of(seatId));

        return request;
    }

    private Booking reserveExistingSeatsFor(Booking booking) {
        LocalDateTime expiresAt = LocalDateTime.now(testClock).plusMinutes(10);

        booking.setReservationExpiresAt(expiresAt);
        Booking saved = bookingRepository.saveAndFlush(booking);

        List<Seat> seats = seatRepository.findByShowIdOrderByIdAsc(
                saved.getShow().getId());

        for (Seat seat : seats) {
            if (saved.getSeatNumbers().contains(seat.getSeatNumber())) {
                seat.setReservedByBookingId(saved.getId());
                seat.setReservedUntil(expiresAt);
            }
        }

        seatRepository.saveAllAndFlush(seats);

        return saved;
    }

    private String capturedPaymentPayload(
            String event,
            String orderId,
            String paymentId,
            long amount,
            String currency) {

        return """
                {"entity":"event","account_id":"acc_test","event":"%s","contains":["payment"],
                 "payload":{"payment":{"entity":{"id":"%s","entity":"payment","amount":%d,
                 "currency":"%s","status":"captured","order_id":"%s","method":"card"}}},
                 "created_at":1700000000}
                """.formatted(event, paymentId, amount, currency, orderId);
    }

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));

        return HexFormat.of().formatHex(
                mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
