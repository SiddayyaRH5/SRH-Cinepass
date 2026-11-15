package com.srh.cinepass.exception;

/**
 * Raised when a Razorpay webhook does not carry a valid HMAC SHA-256
 * signature for the configured webhook secret. The controller maps this to
 * HTTP 401 and never reveals the configured secret.
 */
public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException() {
        super("Invalid Razorpay webhook signature");
    }

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
