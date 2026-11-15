package com.srh.cinepass.dto;

import com.srh.cinepass.entity.Movie;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Administrator view of a movie. It exposes the lifecycle, provenance and
 * synchronisation fields that the public catalogue API deliberately hides.
 *
 * @param id                         internal movie id
 * @param tmdbId                     TMDB identifier, {@code null} for manual movies
 * @param title                      movie title
 * @param status                     lifecycle state, {@code ACTIVE} or {@code ARCHIVED}
 * @param source                     management source, {@code MANUAL} or {@code TMDB}
 * @param releaseDate                effective release date used for show eligibility
 * @param indiaTheatricalReleaseDate confirmed Indian theatrical release date
 * @param excludedFromAutoShows      whether the scheduler must skip this movie
 * @param exclusionReason            why the movie was excluded
 * @param adminProtected             whether automatic retirement must skip this movie
 * @param activeUntil                administrator-managed end of the active period
 * @param retirementHoldReason       why automatic retirement was postponed
 * @param archivedAt                 when the movie was archived
 * @param tmdbSyncedAt               when the metadata was last refreshed
 * @param lastSyncError              last synchronisation error, never a credential
 */
public record MovieAdminView(
        Long id,
        Long tmdbId,
        String title,
        String status,
        String source,
        LocalDate releaseDate,
        LocalDate indiaTheatricalReleaseDate,
        boolean excludedFromAutoShows,
        String exclusionReason,
        boolean adminProtected,
        LocalDate activeUntil,
        String retirementHoldReason,
        LocalDateTime archivedAt,
        LocalDateTime tmdbSyncedAt,
        String lastSyncError) {

    public static MovieAdminView from(Movie movie) {

        if (movie == null) {
            return null;
        }

        return new MovieAdminView(
                movie.getId(),
                movie.getTmdbId(),
                movie.getTitle(),
                movie.getStatus() == null ? "ACTIVE" : movie.getStatus().name(),
                movie.getSource() == null ? "MANUAL" : movie.getSource().name(),
                movie.getReleaseDate(),
                movie.getIndiaTheatricalReleaseDate(),
                movie.isExcludedFromAutoShows(),
                movie.getExclusionReason(),
                movie.isAdminProtected(),
                movie.getActiveUntil(),
                movie.getRetirementHoldReason(),
                movie.getArchivedAt(),
                movie.getTmdbSyncedAt(),
                movie.getLastSyncError());
    }
}
