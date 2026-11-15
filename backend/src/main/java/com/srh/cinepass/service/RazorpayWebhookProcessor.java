package com.srh.cinepass.service;

import com.srh.cinepass.dto.WebhookResult;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.exception.InvalidWebhookPayloadException;
import com.srh.cinepass.repository.BookingRepository;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts an authentic Razorpay event into the shared, idempotent booking
 * confirmation path.
 *
 * <p>Only {@code payment.captured} and {@code order.paid} can confirm a
 * booking. Every other event is ignored. The event's order, amount and
 * currency are checked against the stored booking before
 * {@link BookingService#confirmPaidBooking(String, String)} is called, so a
 * forged or misrouted event can never confirm an unrelated booking or a
 * different amount.</p>
 */
@Service
public class RazorpayWebhookProcessor {

    private static final String EVENT_PAYMENT_CAPTURED = "payment.captured";
    private static final String EVENT_ORDER_PAID = "order.paid";
    private static final String EVENT_PAYMENT_FAILED = "payment.failed";
    private static final String CURRENCY_INR = "INR";

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public RazorpayWebhookProcessor(
            BookingRepository bookingRepository,
            BookingService bookingService) {

        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    public WebhookResult process(String eventId, String eventType, JSONObject event) {

        JSONObject payload = event.optJSONObject("payload");

        if (payload == null) {
            throw new InvalidWebhookPayloadException("Webhook payload is missing");
        }

        if (EVENT_PAYMENT_FAILED.equals(eventType)) {
            /*
             * A failed payment must never issue a ticket. The booking is left
             * untouched: a later captured payment for the same order is the
             * only event allowed to confirm it.
             */
            return WebhookResult.ignored(
                    "Failed payment events do not confirm bookings",
                    eventId);
        }

        if (!EVENT_PAYMENT_CAPTURED.equals(eventType)
                && !EVENT_ORDER_PAID.equals(eventType)) {

            return WebhookResult.ignored(
                    "Event type is not handled by the confirmation endpoint",
                    eventId);
        }

        PaymentDetails details = EVENT_PAYMENT_CAPTURED.equals(eventType)
                ? capturedPaymentDetails(payload)
                : paidOrderDetails(payload);

        if (details.ignoredMessage() != null) {
            return WebhookResult.ignored(details.ignoredMessage(), eventId);
        }

        if (details.orderId() == null || details.paymentId() == null
                || details.amountInPaise() == null || details.currency() == null) {

            throw new InvalidWebhookPayloadException(
                    "The webhook is missing the payment order, amount or currency");
        }

        if (!CURRENCY_INR.equalsIgnoreCase(details.currency())) {
            return WebhookResult.rejected(
                    "Payment currency does not match the booking",
                    null,
                    eventId);
        }

        Booking booking = bookingRepository.findByRazorpayOrderId(details.orderId())
                .orElse(null);

        if (booking == null) {
            return WebhookResult.rejected(
                    "No booking exists for the Razorpay order",
                    null,
                    eventId);
        }

        long expectedAmountInPaise = expectedAmountInPaise(booking);

        if (details.amountInPaise() != expectedAmountInPaise) {
            return WebhookResult.rejected(
                    "Payment amount does not match the booking",
                    booking.getId(),
                    eventId);
        }

        if (booking.getRazorpayPaymentId() != null
                && !booking.getRazorpayPaymentId().isBlank()
                && !booking.getRazorpayPaymentId().equals(details.paymentId())) {

            return WebhookResult.rejected(
                    "Payment does not match the booking",
                    booking.getId(),
                    eventId);
        }

        Booking confirmed = bookingService.confirmPaidBooking(
                details.orderId(),
                details.paymentId());

        if (BookingService.STATUS_PAYMENT_REVIEW.equals(confirmed.getStatus())) {
            return WebhookResult.processed(
                    "Payment captured but the booking needs manual review",
                    confirmed.getId(),
                    eventId);
        }

        return WebhookResult.processed(
                "Payment confirmed",
                confirmed.getId(),
                eventId);
    }

    private PaymentDetails capturedPaymentDetails(JSONObject payload) {

        JSONObject payment = entity(payload, "payment");

        if (payment == null) {
            throw new InvalidWebhookPayloadException(
                    "The payment.captured event is missing the payment entity");
        }

        String status = payment.optString("status", "");

        if (!"captured".equalsIgnoreCase(status)) {
            return PaymentDetails.ignored("Payment is not captured");
        }

        return new PaymentDetails(
                trimToNull(payment.optString("order_id", "")),
                trimToNull(payment.optString("id", "")),
                optLong(payment, "amount"),
                trimToNull(payment.optString("currency", "")),
                null);
    }

    private PaymentDetails paidOrderDetails(JSONObject payload) {

        JSONObject order = entity(payload, "order");
        JSONObject payment = entity(payload, "payment");

        if (order == null) {
            throw new InvalidWebhookPayloadException(
                    "The order.paid event is missing the order entity");
        }

        if (!"paid".equalsIgnoreCase(order.optString("status", ""))) {
            return PaymentDetails.ignored("Order is not paid");
        }

        if (payment == null) {
            throw new InvalidWebhookPayloadException(
                    "The order.paid event is missing the payment entity");
        }

        if (!"captured".equalsIgnoreCase(payment.optString("status", ""))) {
            return PaymentDetails.ignored("Payment is not captured");
        }

        String orderId = trimToNull(order.optString("id", ""));
        String paymentOrderId = trimToNull(payment.optString("order_id", ""));

        if (orderId == null
                || (paymentOrderId != null && !paymentOrderId.equals(orderId))) {

            throw new InvalidWebhookPayloadException(
                    "The payment does not belong to the paid order");
        }

        Long amount = optLong(payment, "amount");

        if (amount == null) {
            amount = optLong(order, "amount");
        }

        String currency = trimToNull(payment.optString("currency", ""));

        if (currency == null) {
            currency = trimToNull(order.optString("currency", ""));
        }

        return new PaymentDetails(
                trimToNull(order.optString("id", "")),
                trimToNull(payment.optString("id", "")),
                amount,
                currency,
                null);
    }

    private JSONObject entity(JSONObject payload, String key) {

        JSONObject wrapper = payload.optJSONObject(key);

        return wrapper == null ? null : wrapper.optJSONObject("entity");
    }

    private Long optLong(JSONObject object, String key) {

        if (object == null || !object.has(key) || object.isNull(key)) {
            return null;
        }

        try {
            return object.getLong(key);

        } catch (JSONException exception) {
            return null;
        }
    }

    private long expectedAmountInPaise(Booking booking) {

        if (booking.getTotalAmount() == null) {
            return -1;
        }

        return BigDecimal.valueOf(booking.getTotalAmount())
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private record PaymentDetails(
            String orderId,
            String paymentId,
            Long amountInPaise,
            String currency,
            String ignoredMessage
    ) {

        static PaymentDetails ignored(String message) {
            return new PaymentDetails(null, null, null, null, message);
        }
    }
}
