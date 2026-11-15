package com.srh.cinepass.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Administrator view of the catalogue synchronisation status.
 *
 * @param totalMovies          every movie row
 * @param tmdbManagedMovies    movies managed by the automatic sync
 * @param manualMovies         movies managed by an administrator
 * @param archivedMovies       retired movies
 * @param excludedMovies       movies excluded from automatic show generation
 * @param moviesWithoutTmdbId  rows that carry no TMDB identifier
 * @param duplicateTmdbIds     TMDB identifiers stored on more than one row
 * @param moviesWithSyncError  movies whose last synchronisation failed
 * @param failedMovies         details of the movies with a sync error
 * @param lastRunAt            when the daily automation last ran
 * @param lastRunOutcome       the outcome of that run
 */
public record MovieSyncStatusView(
        long totalMovies,
        long tmdbManagedMovies,
        long manualMovies,
        long archivedMovies,
        long excludedMovies,
        long moviesWithoutTmdbId,
        List<Long> duplicateTmdbIds,
        long moviesWithSyncError,
        List<MovieAdminView> failedMovies,
        LocalDateTime lastRunAt,
        String lastRunOutcome) {
}
