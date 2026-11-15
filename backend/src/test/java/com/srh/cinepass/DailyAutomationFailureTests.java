package com.srh.cinepass;

import com.srh.cinepass.entity.AutomationRun;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.service.DailyAutomationService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A failing catalogue synchronisation must never stop the rest of the daily
 * automation. The TMDB credential is deliberately absent here, so the
 * synchronisation phase reports a problem while the show generation still
 * completes normally.
 */
@SpringBootTest(properties = {
        "app.movies.sync.enabled=true"
})
class DailyAutomationFailureTests extends TicketTestSupport {

    @Autowired
    private DailyAutomationService automationService;

    @BeforeEach
    void setUp() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 2, 0));
    }

    @Test
    void aFailingCatalogueSyncStillGeneratesTheShows() {

        Movie movie = createTmdbMovie("Current Movie", 9101L,
                LocalDate.of(2026, 11, 1));

        createAdminTheatre("Automation Theatre", 30);

        AutomationRun run = automationService.runDailyAutomation(
                AutomationRun.TRIGGER_SCHEDULED);

        // The run is reported as partial, never as a success.
        assertThat(run.getOutcome()).isEqualTo(AutomationRun.OUTCOME_PARTIAL);
        assertThat(run.getMessage()).isNotBlank();

        // The unrelated show generation still happened.
        assertThat(run.getShowsCreated()).isGreaterThan(0);
        assertThat(showRepository.findByMovieId(movie.getId())).isNotEmpty();
    }
}
