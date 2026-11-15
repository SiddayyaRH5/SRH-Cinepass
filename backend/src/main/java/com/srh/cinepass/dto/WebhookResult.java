package com.srh.cinepass.dto;

/**
 * Outcome of a Razorpay webhook delivery.
 *
 * <p>{@code PROCESSED} means the captured payment was verified and the shared
 * confirmation path was executed (which may itself have parked the booking in
 * manual review). {@code DUPLICATE} means the event ID had already been
 * claimed. {@code IGNORED} means the event does not confirm a payment.
 * {@code REJECTED} means the event was authentic but inconsistent with the
 * stored booking (wrong order, amount or currency), so no ticket was issued.</p>
 */
public record WebhookResult(
        WebhookOutcome status,
        String message,
        Long bookingId,
        String eventId
) {

    public enum WebhookOutcome {
        PROCESSED,
        DUPLICATE,
        IGNORED,
        REJECTED,
        RETRY
    }

    public static WebhookResult processed(String message, Long bookingId, String eventId) {
        return new WebhookResult(WebhookOutcome.PROCESSED, message, bookingId, eventId);
    }

    public static WebhookResult duplicate(String message, Long bookingId, String eventId) {
        return new WebhookResult(WebhookOutcome.DUPLICATE, message, bookingId, eventId);
    }

    public static WebhookResult ignored(String message, String eventId) {
        return new WebhookResult(WebhookOutcome.IGNORED, message, null, eventId);
    }

    public static WebhookResult rejected(String message, Long bookingId, String eventId) {
        return new WebhookResult(WebhookOutcome.REJECTED, message, bookingId, eventId);
    }

    /**
     * A prior delivery is still PROCESSING or previously FAILED. The caller
     * must return a retryable HTTP status so Razorpay delivers the event again
     * instead of treating the duplicate as a successful delivery.
     */
    public static WebhookResult retry(String message, Long bookingId, String eventId) {
        return new WebhookResult(WebhookOutcome.RETRY, message, bookingId, eventId);
    }
}
