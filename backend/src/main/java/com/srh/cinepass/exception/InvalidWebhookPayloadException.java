package com.srh.cinepass.exception;

/**
 * Raised when a signature-verified webhook body is not a usable Razorpay
 * event (malformed JSON, missing payload, missing payment entity, ...).
 * The controller maps this to HTTP 400.
 */
public class InvalidWebhookPayloadException extends RuntimeException {

    public InvalidWebhookPayloadException(String message) {
        super(message);
    }
}
