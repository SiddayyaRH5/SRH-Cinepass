package com.srh.cinepass.service;

import com.srh.cinepass.entity.AutomationRun;
import com.srh.cinepass.repository.AutomationRunRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The daily automation orchestrator.
 *
 * <p>It reuses the existing distributed show-generation lease and runs the
 * phases in a fixed order:</p>
 * <ol>
 *   <li>acquire the lease,</li>
 *   <li>synchronise the TMDB movie catalogue,</li>
 *   <li>evaluate the movie lifecycle and retire eligible movies,</li>
 *   <li>generate the missing shows for the rolling window,</li>
 *   <li>record the statistics and errors,</li>
 *   <li>release the lease.</li>
 * </ol>
 *
 * <p>Each phase is isolated: a failing TMDB call never prevents the show
 * generation, and a failing show generation never prevents the statistics
 * from being written. No database transaction is held open while the TMDB
 * HTTP requests run.</p>
 */
@Service
public class DailyAutomationService {

    private static final Logger log =
            LoggerFactory.getLogger(DailyAutomationService.class);

    private final ShowGenerationLeaseService leaseService;
    private final MovieCatalogueSyncService catalogueSyncService;
    private final MovieLifecycleService lifecycleService;
    private final ShowGenerationService showGenerationService;
    private final AutomationRunRepository automationRunRepository;
    private final Clock clock;

    public DailyAutomationService(
            ShowGenerationLeaseService leaseService,
            MovieCatalogueSyncService catalogueSyncService,
            MovieLifecycleService lifecycleService,
            ShowGenerationService showGenerationService,
            AutomationRunRepository automationRunRepository,
            Clock clock) {

        this.leaseService = leaseService;
        this.catalogueSyncService = catalogueSyncService;
        this.lifecycleService = lifecycleService;
        this.showGenerationService = showGenerationService;
        this.automationRunRepository = automationRunRepository;
        this.clock = clock;
    }

    /**
     * Runs the whole daily automation once.
     *
     * @param triggerType {@code SCHEDULED} or {@code MANUAL}
     * @return the recorded audit row
     */
    public AutomationRun runDailyAutomation(String triggerType) {

        LocalDateTime startedAt = LocalDateTime.now(clock);

        log.info("Daily automation started (trigger={})", triggerType);

        leaseService.ensureLeaseRow();

        String owner = "cinepass-" + UUID.randomUUID();

        if (!leaseService.tryAcquire(owner)) {
            log.info("Daily automation skipped: another instance holds the lease");

            return record(startedAt, triggerType, AutomationRun.OUTCOME_SKIPPED,
                    "lease-not-acquired", null, null, null);
        }

        try {

            MovieCatalogueSyncService.SyncReport sync = null;
            MovieLifecycleService.LifecycleReport lifecycle = null;
            ShowGenerationService.GenerationReport generation = null;

            List<String> errors = new ArrayList<>();

            // 1. TMDB catalogue synchronisation.
            try {
                sync = catalogueSyncService.synchronize();
                errors.addAll(sync.errors());
            } catch (RuntimeException ex) {
                log.error("TMDB catalogue synchronisation failed", ex);
                errors.add("catalogue sync failed: " + safeMessage(ex));
            }

            // 2. Movie lifecycle and retirement.
            try {
                lifecycle = lifecycleService.evaluate();
            } catch (RuntimeException ex) {
                log.error("Movie lifecycle evaluation failed", ex);
                errors.add("lifecycle failed: " + safeMessage(ex));
            }

            // 3. Rolling window show generation.
            try {
                generation = showGenerationService.generateUpcomingShowsWithLeaseHeld();
            } catch (RuntimeException ex) {
                log.error("Show generation failed", ex);
                errors.add("show generation failed: " + safeMessage(ex));
            }

            String outcome = AutomationRun.OUTCOME_SUCCESS;

            if (!errors.isEmpty()) {
                outcome = AutomationRun.OUTCOME_PARTIAL;
            }

            if (sync == null && lifecycle == null && generation == null) {
                outcome = AutomationRun.OUTCOME_FAILED;
            }

            return record(startedAt, triggerType, outcome,
                    summarise(errors), sync, lifecycle, generation);

        } finally {
            leaseService.release(owner);
            log.info("Daily automation finished (trigger={})", triggerType);
        }
    }

    private AutomationRun record(
            LocalDateTime startedAt,
            String triggerType,
            String outcome,
            String message,
            MovieCatalogueSyncService.SyncReport sync,
            MovieLifecycleService.LifecycleReport lifecycle,
            ShowGenerationService.GenerationReport generation) {

        AutomationRun run = new AutomationRun();

        run.setTriggerType(triggerType);
        run.setStartedAt(startedAt);
        run.setFinishedAt(LocalDateTime.now(clock));
        run.setOutcome(outcome);
        run.setMessage(truncate(message));

        run.setPagesProcessed(sync == null ? 0 : sync.pagesProcessed());
        run.setMoviesDiscovered(sync == null ? 0 : sync.discovered());
        run.setMoviesInserted(sync == null ? 0 : sync.inserted());
        run.setMoviesUpdated(sync == null ? 0 : sync.updated());
        run.setMoviesSkipped(sync == null ? 0 : sync.skipped());

        run.setMoviesArchived(lifecycle == null ? 0 : lifecycle.archived());
        run.setMoviesProtected(lifecycle == null ? 0 : lifecycle.protectedCount());

        run.setShowsCreated(generation == null ? 0 : generation.created());
        run.setShowsSkipped(generation == null ? 0 : generation.skipped());
        run.setShowsFailed(generation == null ? 0 : generation.failed());

        try {
            return automationRunRepository.save(run);
        } catch (RuntimeException ex) {
            log.error("Failed to record the automation run", ex);
            return run;
        }
    }

    private static String summarise(List<String> errors) {

        if (errors == null || errors.isEmpty()) {
            return "completed";
        }

        return truncate(String.join("; ", errors));
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private static String truncate(String value) {

        if (value == null) {
            return null;
        }

        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
