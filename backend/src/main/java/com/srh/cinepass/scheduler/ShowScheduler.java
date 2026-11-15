package com.srh.cinepass.scheduler;

import com.srh.cinepass.entity.AutomationRun;
import com.srh.cinepass.service.DailyAutomationService;
import com.srh.cinepass.service.ShowCleanupService;

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
 * timezone. The daily job runs every day at 2:00 AM IST and drives the whole
 * automation: TMDB catalogue synchronisation, movie lifecycle evaluation and
 * the rolling show generation, all under a single distributed lease. The
 * cleanup job is deliberately separate and keeps its existing schedule and
 * behaviour.</p>
 */
@Component
public class ShowScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(ShowScheduler.class);

    private final DailyAutomationService automationService;
    private final ShowCleanupService cleanupService;

    private final boolean generationEnabled;
    private final boolean cleanupEnabled;

    public ShowScheduler(
            DailyAutomationService automationService,
            ShowCleanupService cleanupService,
            @Value("${app.shows.scheduler.enabled:true}") boolean generationEnabled,
            @Value("${app.shows.cleanup.enabled:true}") boolean cleanupEnabled) {

        this.automationService = automationService;
        this.cleanupService = cleanupService;
        this.generationEnabled = generationEnabled;
        this.cleanupEnabled = cleanupEnabled;
    }

    /**
     * The daily automation. It runs every calendar day at 2:00 AM IST and
     * processes the rolling show window (today plus the next six dates).
     */
    @Scheduled(
            cron = "${app.shows.scheduler.cron:0 0 2 * * *}",
            zone = "Asia/Kolkata")
    public void generateDailyShows() {

        if (!generationEnabled) {
            return;
        }

        try {
            automationService.runDailyAutomation(AutomationRun.TRIGGER_SCHEDULED);
        } catch (RuntimeException ex) {
            // Never let a scheduler failure kill the scheduling thread.
            log.error("Scheduled daily automation failed", ex);
        }
    }

    /**
     * Expires shows that started more than the configured grace period ago.
     * The schedule and the behaviour are unchanged.
     */
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
