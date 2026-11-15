package com.srh.cinepass.service;

import com.srh.cinepass.dto.WebhookResult;
import com.srh.cinepass.entity.RazorpayWebhookEvent;
import com.srh.cinepass.exception.InvalidWebhookPayloadException;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Entry point for Razorpay webhook deliveries.
 *
 * <p>Order of operations is deliberate: verify the signature over the exact
 * raw body first, then claim the event ID for idempotency, then run the shared
 * confirmation path. An unauthenticated or replayed delivery therefore cannot
 * reach the booking service.</p>
 */
@Service
public class RazorpayWebhookService {

    private static final int MAX_EVENT_ID_LENGTH = 120;

    private final RazorpayWebhookVerifier verifier;
    private final RazorpayWebhookEventStore eventStore;
    private final RazorpayWebhookProcessor processor;

    public RazorpayWebhookService(
            RazorpayWebhookVerifier verifier,
            RazorpayWebhookEventStore eventStore,
            RazorpayWebhookProcessor processor) {

        this.verifier = verifier;
        this.eventStore = eventStore;
        this.processor = processor;
    }

    public WebhookResult handle(
            byte[] rawBody,
            String signature,
            String eventIdHeader) {

        verifier.verify(rawBody, signature);

        JSONObject event = parse(rawBody);

        String eventType = event.optString("event", "").trim();

        if (eventType.isEmpty()) {
            throw new InvalidWebhookPayloadException(
                    "Webhook event type is missing");
        }

        String eventId = resolveEventId(eventIdHeader, rawBody);

        RazorpayWebhookEventStore.Claim claim = eventStore.claim(eventId, eventType);

        if (!claim.claimed()) {
            return duplicateResult(eventId, claim.event());
        }

        try {
            WebhookResult result = processor.process(eventId, eventType, event);

            eventStore.complete(eventId, result, result.bookingId());

            return result;

        } catch (InvalidWebhookPayloadException invalidPayload) {
            /*
             * A permanent payload problem will not succeed on retry, so the
             * event is recorded as rejected rather than failed.
             */
            eventStore.complete(
                    eventId,
                    WebhookResult.rejected(invalidPayload.getMessage(), null, eventId),
                    null);

            throw invalidPayload;

        } catch (RuntimeException transientFailure) {
            eventStore.fail(eventId, safeMessage(transientFailure));
            throw transientFailure;
        }
    }

    private JSONObject parse(byte[] rawBody) {

        if (rawBody == null || rawBody.length == 0) {
            throw new InvalidWebhookPayloadException(
                    "Webhook request body is required");
        }

        try {
            return new JSONObject(new String(rawBody, StandardCharsets.UTF_8));

        } catch (JSONException malformed) {
            throw new InvalidWebhookPayloadException(
                    "Webhook payload is not valid JSON");
        }
    }

    private WebhookResult duplicateResult(
            String eventId,
            RazorpayWebhookEvent event) {

        if (event == null) {
            /*
             * The claim row disappeared between the claim and the duplicate
             * lookup. Treat this as retryable rather than acknowledging an
             * event whose outcome is unknown.
             */
            return WebhookResult.retry(
                    "Webhook event state is unavailable",
                    null,
                    eventId);
        }

        Long bookingId = event.getBookingId();
        String status = event.getStatus();

        if ("PROCESSED".equals(status)) {
            return WebhookResult.duplicate(
                    "Webhook event was already processed",
                    bookingId,
                    eventId);
        }

        /*
         * FAILED means the previous attempt rolled back and is safe to retry.
         * PROCESSING means another delivery may still be in flight, or a
         * previous process crashed. In both cases returning 200 would tell
         * Razorpay to stop retrying a payment that may never have been
         * confirmed, so the controller must return a retryable status.
         */
        if ("FAILED".equals(status)) {
            return WebhookResult.retry(
                    "Webhook event previously failed and is eligible for retry",
                    bookingId,
                    eventId);
        }

        if ("PROCESSING".equals(status)) {
            return WebhookResult.retry(
                    "Webhook event is still being processed",
                    bookingId,
                    eventId);
        }

        /*
         * IGNORED and REJECTED are terminal outcomes: retrying cannot change
         * them, so acknowledge the duplicate with 200 instead of asking
         * Razorpay to redeliver forever.
         */
        return WebhookResult.duplicate(
                "Webhook event was already handled",
                bookingId,
                eventId);
    }

    /**
     * Razorpay always sends {@code X-Razorpay-Event-Id}. If a proxy strips it,
     * a deterministic hash of the signed body still gives replay protection.
     */
    private String resolveEventId(String eventIdHeader, byte[] rawBody) {

        if (eventIdHeader != null && !eventIdHeader.isBlank()) {

            String trimmed = eventIdHeader.trim();

            if (trimmed.length() <= MAX_EVENT_ID_LENGTH) {
                return trimmed;
            }
        }

        return "sha256:" + sha256Hex(rawBody);
    }

    private String sha256Hex(byte[] rawBody) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawBody));

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable");
        }
    }

    private String safeMessage(RuntimeException exception) {

        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Webhook processing failed";
        }

        return exception.getMessage();
    }
}
