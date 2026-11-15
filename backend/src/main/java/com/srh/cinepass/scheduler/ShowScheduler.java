package com.srh.cinepass.scheduler;

import com.srh.cinepass.service.ShowCleanupService;
import com.srh.cinepass.service.ShowGenerationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thin scheduled entry points. All business logic lives in the services so the
 * jobs can also be triggered manually or from tests.
 *
 * <p>Cron expressions are evaluated in Asia/Kolkata regardless of the JVM
 * timezone. Both jobs are disabled by configuration in tests.</p>
 */
@Component
public class ShowScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ShowScheduler.class);

    private final ShowGenerationService generationService;
    private final ShowCleanupService cleanupService;

    private final boolean generationEnabled;
    private final boolean cleanupEnabled;

    public ShowScheduler(
            ShowGenerationService generationService,
            ShowCleanupService cleanupService,
            @Value("${app.shows.scheduler.enabled:true}") boolean generationEnabled,
            @Value("${app.shows.cleanup.enabled:true}") boolean cleanupEnabled) {

        this.generationService = generationService;
        this.cleanupService = cleanupService;
        this.generationEnabled = generationEnabled;
        this.cleanupEnabled = cleanupEnabled;
    }

    @Scheduled(
            cron = "${app.shows.scheduler.cron:0 0 2 * * *}",
            zone = "Asia/Kolkata")
    public void generateDailyShows() {

        if (!generationEnabled) {
            return;
        }

        try {
            generationService.generateUpcomingShows();
        } catch (RuntimeException ex) {
            // Never let a scheduler failure kill the scheduling thread.
            log.error("Scheduled show generation failed", ex);
        }
    }

    @Scheduled(
            cron = "${app.shows.cleanup.cron:0 */10 * * * *}",
            zone = "Asia/Kolkata")
    public void expireFinishedShows() {

        if (!cleanupEnabled) {
            return;
        }

        try {
            cleanupService.expireFinishedShows();
        } catch (RuntimeException ex) {
            log.error("Scheduled show cleanup failed", ex);
        }
    }
}
