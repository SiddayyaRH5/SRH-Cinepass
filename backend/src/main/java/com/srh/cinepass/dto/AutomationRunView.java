package com.srh.cinepass.dto;

import com.srh.cinepass.entity.AutomationRun;

import java.time.LocalDateTime;

/**
 * Administrator view of one daily automation run.
 *
 * @param id                run id
 * @param triggerType       {@code SCHEDULED} or {@code MANUAL}
 * @param startedAt         when the run started
 * @param finishedAt        when the run finished
 * @param outcome           {@code SUCCESS}, {@code PARTIAL}, {@code FAILED} or {@code SKIPPED}
 * @param durationSeconds   wall-clock duration of the run
 * @param pagesProcessed    TMDB pages that were read
 * @param moviesDiscovered  movies returned by the TMDB discovery lists
 * @param moviesInserted    movies added to the catalogue
 * @param moviesUpdated     movies whose metadata was refreshed
 * @param moviesSkipped     movies that were deliberately not changed
 * @param moviesArchived    movies retired by the lifecycle job
 * @param moviesProtected   movies whose retirement was postponed
 * @param showsCreated      shows generated
 * @param showsSkipped      shows that already existed or were not eligible
 * @param showsFailed       shows that could not be generated
 * @param message           short, credential-free summary
 */
public record AutomationRunView(
        Long id,
        String triggerType,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String outcome,
        long durationSeconds,
        int pagesProcessed,
        int moviesDiscovered,
        int moviesInserted,
        int moviesUpdated,
        int moviesSkipped,
        int moviesArchived,
        int moviesProtected,
        int showsCreated,
        int showsSkipped,
        int showsFailed,
        String message) {

    public static AutomationRunView from(AutomationRun run) {

        if (run == null) {
            return null;
        }

        long duration = 0;

        if (run.getStartedAt() != null && run.getFinishedAt() != null) {
            duration = java.time.Duration
                    .between(run.getStartedAt(), run.getFinishedAt())
                    .getSeconds();
        }

        return new AutomationRunView(
                run.getId(),
                run.getTriggerType(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getOutcome(),
                duration,
                value(run.getPagesProcessed()),
                value(run.getMoviesDiscovered()),
                value(run.getMoviesInserted()),
                value(run.getMoviesUpdated()),
                value(run.getMoviesSkipped()),
                value(run.getMoviesArchived()),
                value(run.getMoviesProtected()),
                value(run.getShowsCreated()),
                value(run.getShowsSkipped()),
                value(run.getShowsFailed()),
                run.getMessage());
    }

    private static int value(Integer number) {
        return number == null ? 0 : number;
    }
}
