package com.srh.cinepass.controller;

import com.srh.cinepass.dto.WebhookResult;
import com.srh.cinepass.exception.InvalidWebhookPayloadException;
import com.srh.cinepass.exception.InvalidWebhookSignatureException;
import com.srh.cinepass.service.RazorpayWebhookService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Razorpay's server-to-server callback endpoint.
 *
 * <p>It is publicly reachable (Razorpay cannot present a customer JWT) but is
 * authenticated by the HMAC SHA-256 signature over the exact raw request
 * body. It never returns ticket credentials and never trusts unsigned JSON.</p>
 */
@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayWebhookController {

    /*
     * Razorpay events are small. Capping the raw body protects the public
     * endpoint from memory exhaustion before HMAC verification runs.
     */
    private static final int MAX_WEBHOOK_BODY_BYTES = 64 * 1024;

    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(RazorpayWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId)
            throws IOException {

        byte[] rawBody = readRawBody(request);

        WebhookResult result = webhookService.handle(rawBody, signature, eventId);

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("status", result.status().name());
        body.put("message", result.message());
        body.put("eventId", result.eventId());

        if (result.bookingId() != null) {
            body.put("bookingId", result.bookingId());
        }

        if (result.status() == WebhookResult.WebhookOutcome.RETRY) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }

        return ResponseEntity.ok(body);
    }

    private byte[] readRawBody(HttpServletRequest request) throws IOException {

        long declaredLength = request.getContentLengthLong();

        if (declaredLength > MAX_WEBHOOK_BODY_BYTES) {
            throw new InvalidWebhookPayloadException(
                    "Webhook payload is too large");
        }

        byte[] body = request.getInputStream()
                .readNBytes(MAX_WEBHOOK_BODY_BYTES + 1);

        if (body.length > MAX_WEBHOOK_BODY_BYTES) {
            throw new InvalidWebhookPayloadException(
                    "Webhook payload is too large");
        }

        return body;
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    public ResponseEntity<Map<String, String>> invalidSignature(
            InvalidWebhookSignatureException exception) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "REJECTED", "message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidWebhookPayloadException.class)
    public ResponseEntity<Map<String, String>> invalidPayload(
            InvalidWebhookPayloadException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of("status", "REJECTED", "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> notConfigured(
            IllegalStateException exception) {

        /*
         * Never echo the underlying message: it may name environment
         * variables or internal state. Razorpay sees a retryable 500.
         */
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", "FAILED",
                        "message", "Webhook processing is not available"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> failed(Exception exception) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", "FAILED",
                        "message", "Webhook could not be processed"));
    }
}
