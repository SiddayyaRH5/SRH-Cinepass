package com.srh.cinepass;

import com.srh.cinepass.dto.WebhookResult;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.RazorpayWebhookEvent;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.service.RazorpayWebhookService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RazorpayWebhookTests extends TicketTestSupport {

    private static final String WEBHOOK_SECRET = "test_webhook_secret";
    private static final long AMOUNT_IN_PAISE = 50_000L;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RazorpayWebhookService webhookService;

    @Test
    void validCapturedPaymentSignatureConfirmsTheBookingAndIssuesOneTicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_valid_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_valid_1", "pay_valid_1", AMOUNT_IN_PAISE, "INR");

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_valid_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.bookingId").value(booking.getId()));

        Booking confirmed = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(confirmed.getRazorpayPaymentId()).isEqualTo("pay_valid_1");
        assertThat(ticketRepository.findByBookingId(booking.getId())).isPresent();
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void orderPaidEventAlsoConfirmsTheBooking() throws Exception {
        Booking booking = pendingBookingWithOrder("order_paid_event_1");

        String payload = paidOrderPayload(
                "order_paid_event_1", "pay_order_paid_1", AMOUNT_IN_PAISE, "INR");

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_order_paid_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("CONFIRMED");
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidSignatureIsRejectedWithoutIssuingATicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_invalid_sig_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_invalid_sig_1", "pay_invalid_sig_1",
                AMOUNT_IN_PAISE, "INR");

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "deadbeef")
                        .header("X-Razorpay-Event-Id", "evt_invalid_sig_1")
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void missingSignatureIsRejectedWithoutIssuingATicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_missing_sig_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_missing_sig_1", "pay_missing_sig_1",
                AMOUNT_IN_PAISE, "INR");

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Event-Id", "evt_missing_sig_1")
                        .content(payload))
                .andExpect(status().isUnauthorized());

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void duplicateEventIdIsIdempotentAndNeverIssuesASecondTicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_duplicate_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_duplicate_1", "pay_duplicate_1",
                AMOUNT_IN_PAISE, "INR");

        String signature = sign(payload);

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", "evt_duplicate_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", "evt_duplicate_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("CONFIRMED");
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateIgnoredEventIsAcknowledgedWithoutInfiniteRetries() throws Exception {
        Booking booking = pendingBookingWithOrder("order_dup_ignored_1");

        String payload = failedPaymentPayload(
                "order_dup_ignored_1", "pay_dup_ignored_1", AMOUNT_IN_PAISE, "INR");

        String signature = sign(payload);

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", "evt_dup_ignored_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"));

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", "evt_dup_ignored_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void duplicateRejectedEventIsAcknowledgedWithoutInfiniteRetries() throws Exception {
        Booking booking = pendingBookingWithOrder("order_dup_rejected_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_dup_rejected_1", "pay_dup_rejected_1",
                AMOUNT_IN_PAISE - 100, "INR");

        String signature = sign(payload);

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", "evt_dup_rejected_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", "evt_dup_rejected_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void previouslyFailedEventCanRetryImmediatelyAndConfirmsTheBooking() throws Exception {
        Booking booking = pendingBookingWithOrder("order_retry_failed_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_retry_failed_1", "pay_retry_failed_1",
                AMOUNT_IN_PAISE, "INR");

        seedWebhookEvent(
                "evt_retry_failed_1",
                "FAILED",
                LocalDateTime.now(testClock));

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_retry_failed_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.bookingId").value(booking.getId()));

        Booking confirmed = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(ticketRepository.count()).isEqualTo(1);

        RazorpayWebhookEvent retried = webhookEventRepository
                .findByEventId("evt_retry_failed_1")
                .orElseThrow();

        assertThat(retried.getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    void eventStillProcessingWithinTheCoolOffReturnsRetryable503() throws Exception {
        Booking booking = pendingBookingWithOrder("order_retry_processing_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_retry_processing_1", "pay_retry_processing_1",
                AMOUNT_IN_PAISE, "INR");

        seedWebhookEvent(
                "evt_retry_processing_1",
                "PROCESSING",
                LocalDateTime.now(testClock));

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_retry_processing_1")
                        .content(payload))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("RETRY"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();

        assertThat(webhookEventRepository.findByEventId("evt_retry_processing_1")
                .orElseThrow()
                .getStatus())
                .isEqualTo("PROCESSING");
    }

    @Test
    void eventLeftProcessingAfterTheCoolOffCanBeReclaimedAndConfirmed() throws Exception {
        Booking booking = pendingBookingWithOrder("order_retry_reclaim_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_retry_reclaim_1", "pay_retry_reclaim_1",
                AMOUNT_IN_PAISE, "INR");

        seedWebhookEvent(
                "evt_retry_reclaim_1",
                "PROCESSING",
                LocalDateTime.now(testClock).minusMinutes(6));

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_retry_reclaim_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("CONFIRMED");
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void oversizedWebhookBodyIsRejectedBeforeSignatureVerification() throws Exception {
        String payload = "{\"event\":\"payment.captured\",\"padding\":\""
                + "x".repeat(70_000)
                + "\"}";

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_oversized_1")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Webhook payload is too large"));

        assertThat(webhookEventRepository.findByEventId("evt_oversized_1")).isEmpty();
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void failedPaymentEventNeverIssuesATicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_failed_1");

        String payload = failedPaymentPayload(
                "order_failed_1", "pay_failed_1", AMOUNT_IN_PAISE, "INR");

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_failed_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void incorrectAmountIsRejectedWithoutIssuingATicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_wrong_amount_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_wrong_amount_1", "pay_wrong_amount_1",
                AMOUNT_IN_PAISE - 100, "INR");

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_wrong_amount_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void incorrectOrderReferenceIsRejectedWithoutIssuingATicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_known_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_unknown_1", "pay_unknown_order_1",
                AMOUNT_IN_PAISE, "INR");

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", sign(payload))
                        .header("X-Razorpay-Event-Id", "evt_unknown_order_1")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("PENDING");
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void browserAndWebhookConfirmationRaceStillProducesExactlyOneTicket() throws Exception {
        Booking booking = pendingBookingWithOrder("order_race_1");

        String payload = capturedPaymentPayload(
                "payment.captured", "order_race_1", "pay_race_1",
                AMOUNT_IN_PAISE, "INR");

        String signature = sign(payload);
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<WebhookResult> webhookCall = () -> {
                start.await(10, TimeUnit.SECONDS);
                return webhookService.handle(body, signature, "evt_race_1");
            };

            Callable<Booking> browserCall = () -> {
                start.await(10, TimeUnit.SECONDS);
                return bookingService.confirmPaidBooking("order_race_1", "pay_race_1");
            };

            Future<WebhookResult> webhookResult = executor.submit(webhookCall);
            Future<Booking> browserResult = executor.submit(browserCall);

            start.countDown();

            WebhookResult result = webhookResult.get(30, TimeUnit.SECONDS);
            Booking browserBooking = browserResult.get(30, TimeUnit.SECONDS);

            assertThat(result.status()).isIn(
                    WebhookResult.WebhookOutcome.PROCESSED,
                    WebhookResult.WebhookOutcome.DUPLICATE);
            assertThat(browserBooking.getStatus()).isEqualTo("CONFIRMED");

        } finally {
            executor.shutdownNow();
        }

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo("CONFIRMED");
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    private Booking pendingBookingWithOrder(String orderId) {
        User customer = createUser("Webhook Customer " + orderId, "USER");
        Movie movie = createMovie("Webhook Movie " + orderId);
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking booking = createBooking(customer, show, "PENDING", "PENDING");
        booking.setRazorpayOrderId(orderId);
        bookingRepository.saveAndFlush(booking);

        return reserveSeatsForPayment(booking);
    }

    private void seedWebhookEvent(
            String eventId,
            String status,
            LocalDateTime receivedAt) {

        RazorpayWebhookEvent event = new RazorpayWebhookEvent();
        event.setEventId(eventId);
        event.setEventType("payment.captured");
        event.setStatus(status);
        event.setReceivedAt(receivedAt);

        webhookEventRepository.saveAndFlush(event);
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

    private String paidOrderPayload(
            String orderId,
            String paymentId,
            long amount,
            String currency) {

        return """
                {"entity":"event","account_id":"acc_test","event":"order.paid","contains":["order","payment"],
                 "payload":{
                   "order":{"entity":{"id":"%s","entity":"order","amount":%d,"currency":"%s","status":"paid"}},
                   "payment":{"entity":{"id":"%s","entity":"payment","amount":%d,"currency":"%s","status":"captured","order_id":"%s"}}
                 },
                 "created_at":1700000000}
                """.formatted(orderId, amount, currency, paymentId, amount, currency, orderId);
    }

    private String failedPaymentPayload(
            String orderId,
            String paymentId,
            long amount,
            String currency) {

        return """
                {"entity":"event","account_id":"acc_test","event":"payment.failed","contains":["payment"],
                 "payload":{"payment":{"entity":{"id":"%s","entity":"payment","amount":%d,
                 "currency":"%s","status":"failed","order_id":"%s","error_code":"BAD_REQUEST"}}},
                 "created_at":1700000000}
                """.formatted(paymentId, amount, currency, orderId);
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
