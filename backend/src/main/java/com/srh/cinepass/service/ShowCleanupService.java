package com.srh.cinepass.service;

import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.repository.ShowRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Moves shows past their grace period from SCHEDULED to EXPIRED.
 *
 * <p>This is a status change only. No show, booking, ticket, seat or payment
 * row is deleted, and payment status is never touched. Already-issued tickets
 * remain valid according to the ticket admission window. Running the job twice
 * is a no-op because the bulk update only targets SCHEDULED (or legacy null)
 * rows.</p>
 */
@Service
public class ShowCleanupService {

    private static final Logger log =
            LoggerFactory.getLogger(ShowCleanupService.class);

    private final ShowRepository showRepository;
    private final Clock clock;
    private final int minutesAfterStart;

    public ShowCleanupService(
            ShowRepository showRepository,
            Clock clock,
            @Value("${show.cleanup.minutes-after-start:45}") int minutesAfterStart) {

        this.showRepository = showRepository;
        this.clock = clock;
        this.minutesAfterStart = Math.max(0, minutesAfterStart);
    }

    /**
     * @return the number of shows transitioned to EXPIRED by this run.
     */
    @Transactional
    public int expireFinishedShows() {

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime threshold = now.minusMinutes(minutesAfterStart);

        int updated = showRepository.markExpired(
                threshold,
                ShowStatus.EXPIRED,
                ShowStatus.SCHEDULED);

        if (updated > 0) {
            log.info(
                    "Show cleanup marked {} show(s) as EXPIRED (threshold={})",
                    updated,
                    threshold);
        }

        return updated;
    }

    public int getMinutesAfterStart() {
        return minutesAfterStart;
    }
}
