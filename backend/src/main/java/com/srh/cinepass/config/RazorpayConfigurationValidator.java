package com.srh.cinepass.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fails fast on Render when a production deployment is missing the Razorpay
 * configuration. Error messages name the environment variables but never
 * include their values.
 */
@Component
public class RazorpayConfigurationValidator implements InitializingBean {

    private final String environment;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayConfigurationValidator(
            @Value("${app.environment:development}") String environment,
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret,
            @Value("${razorpay.webhook-secret:}") String webhookSecret) {

        this.environment = environment;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public void afterPropertiesSet() {

        if (!"production".equalsIgnoreCase(trim(environment))) {
            return;
        }

        List<String> missing = new ArrayList<>();

        if (isBlank(keyId)) {
            missing.add("RAZORPAY_KEY_ID");
        }

        if (isBlank(keySecret)) {
            missing.add("RAZORPAY_KEY_SECRET");
        }

        if (isBlank(webhookSecret)) {
            missing.add("RAZORPAY_WEBHOOK_SECRET");
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required production configuration: "
                            + String.join(", ", missing)
                            + ". Set these environment variables in the deployment platform.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
