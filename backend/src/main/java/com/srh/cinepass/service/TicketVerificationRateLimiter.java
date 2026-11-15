package com.srh.cinepass.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.srh.cinepass.exception.RateLimitExceededException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * In-process fixed-window limiter scoped to the authenticated staff account.
 * This is appropriate for the existing single-instance Render deployment and
 * adds no external infrastructure. A multi-instance deployment should replace
 * it with a shared limiter (for example Redis).
 */
@Component
public class TicketVerificationRateLimiter {

    private final int maxAttemptsPerMinute;
    private final Cache<Long, Window> windows;

    public TicketVerificationRateLimiter(
            @Value("${app.tickets.verify-rate-limit-per-minute:60}") int maxAttemptsPerMinute) {

        this.maxAttemptsPerMinute = Math.max(1, maxAttemptsPerMinute);
        this.windows = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    public void check(Long staffId) {
        if (staffId == null) {
            return;
        }

        Window window = windows.get(staffId, key -> new Window());

        synchronized (window) {
            if (window.count >= maxAttemptsPerMinute) {
                throw new RateLimitExceededException();
            }

            window.count++;
        }
    }

    private static final class Window {
        private int count;
    }
}
