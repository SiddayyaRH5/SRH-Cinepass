package com.srh.cinepass;

import com.srh.cinepass.entity.AutomationRun;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.repository.AutomationRunRepository;
import com.srh.cinepass.service.DailyAutomationService;
import com.srh.cinepass.service.ShowGenerationLeaseService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The daily automation orchestrator.
 *
 * <p>The phases run in a fixed order under a single distributed lease:
 * catalogue synchronisation, lifecycle evaluation, show generation. The run
 * statistics are recorded for the administrator.</p>
 */
@SpringBootTest(properties = {
        "app.shows.scheduler.enabled=true",
        "app.movies.sync.enabled=false"
})
class DailyAutomationServiceTests extends TicketTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    @Autowired
    private DailyAutomationService automationService;

    @Autowired
    private AutomationRunRepository automationRunRepository;

    @Autowired
    private ShowGenerationLeaseService leaseService;

    @BeforeEach
    void setUp() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 2, 0));
    }

    @Test
    void runsTheLifecycleBeforeTheShowGeneration() {

        Movie current = createTmdbMovie("Current Movie", 9001L, TODAY.minusDays(10));
        Movie old = createTmdbMovie("Old Movie", 9002L, TODAY.minusDays(45));

        Theatre theatre = createAdminTheatre("Automation Theatre", 30);

        AutomationRun run = automationService.runDailyAutomation(
                AutomationRun.TRIGGER_SCHEDULED);

        assertThat(run.getOutcome()).isEqualTo(AutomationRun.OUTCOME_SUCCESS);
        assertThat(run.getTriggerType()).isEqualTo(AutomationRun.TRIGGER_SCHEDULED);

        // The lifecycle ran first, so the retired movie never reached a screen.
        assertThat(movieRepository.findById(old.getId()).orElseThrow().getStatus())
                .isEqualTo(MovieStatus.ARCHIVED);
        assertThat(showRepository.findByMovieId(old.getId())).isEmpty();

        // The active movie still received its shows.
        assertThat(showRepository.findByMovieId(current.getId())).isNotEmpty();
        assertThat(showRepository.findByTheatreId(theatre.getId())).isNotEmpty();

        assertThat(run.getMoviesArchived()).isEqualTo(1);
        assertThat(run.getShowsCreated()).isGreaterThan(0);
    }

    @Test
    void recordsTheRunStatisticsForTheAdministrator() {

        createTmdbMovie("Current Movie", 9003L, TODAY.minusDays(10));
        createAdminTheatre("Automation Theatre", 30);

        AutomationRun run = automationService.runDailyAutomation(
                AutomationRun.TRIGGER_MANUAL);

        List<AutomationRun> runs =
                automationRunRepository.findTop50ByOrderByStartedAtDesc();

        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getId()).isEqualTo(run.getId());
        assertThat(runs.get(0).getOutcome()).isEqualTo(AutomationRun.OUTCOME_SUCCESS);
        assertThat(runs.get(0).getStartedAt()).isNotNull();
        assertThat(runs.get(0).getFinishedAt()).isNotNull();
        assertThat(runs.get(0).getShowsCreated()).isEqualTo(run.getShowsCreated());
        assertThat(runs.get(0).getShowsFailed()).isZero();
    }

    @Test
    void skipsTheRunWhenAnotherInstanceHoldsTheLease() {

        createTmdbMovie("Current Movie", 9004L, TODAY.minusDays(10));
        createAdminTheatre("Automation Theatre", 30);

        leaseService.ensureLeaseRow();

        assertThat(leaseService.tryAcquire("another-instance")).isTrue();

        AutomationRun run = automationService.runDailyAutomation(
                AutomationRun.TRIGGER_SCHEDULED);

        assertThat(run.getOutcome()).isEqualTo(AutomationRun.OUTCOME_SKIPPED);
        assertThat(run.getMessage()).contains("lease");
        assertThat(showRepository.count()).isZero();

        leaseService.release("another-instance");
    }

    @Test
    void releasesTheLeaseSoTheNextRunCanStart() {

        createTmdbMovie("Current Movie", 9005L, TODAY.minusDays(10));
        createAdminTheatre("Automation Theatre", 30);

        automationService.runDailyAutomation(AutomationRun.TRIGGER_SCHEDULED);

        AutomationRun second = automationService.runDailyAutomation(
                AutomationRun.TRIGGER_SCHEDULED);

        assertThat(second.getOutcome()).isEqualTo(AutomationRun.OUTCOME_SUCCESS);
    }

    @Test
    void isSafeToRunTwiceWithoutDuplicatingAnything() {

        createTmdbMovie("Current Movie", 9006L, TODAY.minusDays(10));
        createAdminTheatre("Automation Theatre", 30);

        automationService.runDailyAutomation(AutomationRun.TRIGGER_SCHEDULED);

        long shows = showRepository.count();
        long seats = seatRepository.count();

        automationService.runDailyAutomation(AutomationRun.TRIGGER_SCHEDULED);

        assertThat(showRepository.count()).isEqualTo(shows);
        assertThat(seatRepository.count()).isEqualTo(seats);
    }
}
