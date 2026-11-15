package com.srh.cinepass.service;

import com.srh.cinepass.dto.WebhookResult;
import com.srh.cinepass.entity.RazorpayWebhookEvent;
import com.srh.cinepass.repository.RazorpayWebhookEventRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Persists webhook event IDs and their outcomes.
 *
 * <p>Each operation runs in its own short {@code REQUIRES_NEW} transaction so
 * that a duplicate-key race cannot roll back the payment confirmation, and so
 * that the audit row is committed even if the payment transaction fails. The
 * unique constraint on {@code event_id} is the final arbiter when two
 * deliveries of the same event arrive concurrently.</p>
 */
@Service
public class RazorpayWebhookEventStore {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final long RETRY_FAILED_AFTER_MINUTES = 5;

    private final RazorpayWebhookEventRepository repository;
    private final TransactionTemplate requiresNew;
    private final Clock clock;

    public RazorpayWebhookEventStore(
            RazorpayWebhookEventRepository repository,
            PlatformTransactionManager transactionManager,
            Clock clock) {

        this.repository = repository;
        this.clock = clock;

        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Claim claim(String eventId, String eventType) {

        try {
            return requiresNew.execute(status -> doClaim(eventId, eventType));

        } catch (DataIntegrityViolationException concurrentInsert) {
            /*
             * Another delivery claimed the same event ID between our lookup
             * and insert. The unique constraint rejected this row, so this
             * delivery must not process the payment again.
             */
            return new Claim(false, repository.findByEventId(eventId).orElse(null));
        }
    }

    public void complete(
            String eventId,
            WebhookResult result,
            Long bookingId) {

        requiresNew.executeWithoutResult(status -> {

            RazorpayWebhookEvent event = repository.findByEventId(eventId).orElse(null);

            if (event == null) {
                return;
            }

            event.setStatus(result.status().name());
            event.setMessage(truncate(result.message()));
            event.setBookingId(bookingId);
            event.setProcessedAt(LocalDateTime.now(clock));

            repository.saveAndFlush(event);
        });
    }

    public void fail(String eventId, String message) {

        requiresNew.executeWithoutResult(status -> {

            RazorpayWebhookEvent event = repository.findByEventId(eventId).orElse(null);

            if (event == null) {
                return;
            }

            event.setStatus("FAILED");
            event.setMessage(truncate(message));
            event.setProcessedAt(LocalDateTime.now(clock));

            repository.saveAndFlush(event);
        });
    }

    private Claim doClaim(String eventId, String eventType) {

        LocalDateTime now = LocalDateTime.now(clock);

        Optional<RazorpayWebhookEvent> existing = repository.findByEventId(eventId);

        if (existing.isPresent()) {

            RazorpayWebhookEvent event = existing.get();

            /*
             * A failed delivery, or one left in PROCESSING by a crashed
             * instance, may be retried by Razorpay. Only retry after a
             * cool-off so a tight retry loop cannot race the original
             * attempt. Confirmation itself is idempotent, so a retry cannot
             * issue a second ticket.
             */
            boolean failed = "FAILED".equals(event.getStatus());
            boolean processing = "PROCESSING".equals(event.getStatus());
            boolean retryable = failed || processing;

            /*
             * A FAILED attempt has already rolled back and is safe to retry
             * immediately. PROCESSING may still be in flight, or may belong
             * to a crashed instance, so it uses a cool-off before reclaiming.
             */
            boolean retryAllowed = failed
                    || (processing
                            && event.getReceivedAt() != null
                            && event.getReceivedAt().isBefore(now.minusMinutes(RETRY_FAILED_AFTER_MINUTES)));

            if (retryable && retryAllowed) {

                event.setStatus("PROCESSING");
                event.setEventType(eventType);
                event.setMessage(null);
                event.setProcessedAt(null);
                event.setReceivedAt(now);

                repository.saveAndFlush(event);

                return new Claim(true, event);
            }

            return new Claim(false, event);
        }

        RazorpayWebhookEvent event = new RazorpayWebhookEvent();

        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setStatus("PROCESSING");
        event.setReceivedAt(now);

        repository.saveAndFlush(event);

        return new Claim(true, event);
    }

    private String truncate(String message) {

        if (message == null) {
            return null;
        }

        return message.length() <= MAX_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_MESSAGE_LENGTH);
    }

    public record Claim(boolean claimed, RazorpayWebhookEvent event) {
    }
}
