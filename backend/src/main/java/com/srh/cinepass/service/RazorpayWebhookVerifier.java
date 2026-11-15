package com.srh.cinepass.service;

import com.srh.cinepass.exception.InvalidWebhookPayloadException;
import com.srh.cinepass.exception.InvalidWebhookSignatureException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies the {@code X-Razorpay-Signature} header exactly as Razorpay
 * documents it: HMAC SHA-256 over the raw request body using the webhook
 * secret, compared in constant time.
 *
 * <p>The raw bytes of the request body must be used, not a re-serialized JSON
 * document, otherwise harmless formatting differences would break the
 * signature.</p>
 */
@Service
public class RazorpayWebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String webhookSecret;

    public RazorpayWebhookVerifier(
            @Value("${razorpay.webhook-secret:}") String webhookSecret) {

        this.webhookSecret = webhookSecret;
    }

    public void verify(byte[] rawBody, String signature) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            /*
             * Production startup validation already rejects this state. In a
             * non-production environment an unconfigured secret must still
             * fail closed rather than accept arbitrary webhook bodies.
             */
            throw new IllegalStateException(
                    "RAZORPAY_WEBHOOK_SECRET is not configured; webhook deliveries are rejected");
        }

        if (rawBody == null || rawBody.length == 0) {
            throw new InvalidWebhookPayloadException("Webhook request body is required");
        }

        if (signature == null || signature.isBlank()) {
            throw new InvalidWebhookSignatureException();
        }

        byte[] expected = hmac(rawBody);

        byte[] supplied;

        try {
            supplied = HexFormat.of().parseHex(signature.trim());
        } catch (IllegalArgumentException malformedSignature) {
            throw new InvalidWebhookSignatureException();
        }

        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new InvalidWebhookSignatureException();
        }
    }

    private byte[] hmac(byte[] rawBody) {

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));

            return mac.doFinal(rawBody);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to verify the Razorpay webhook signature");
        }
    }
}
