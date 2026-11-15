package com.srh.cinepass.service;

import com.srh.cinepass.dto.MovieAdminView;
import com.srh.cinepass.dto.MovieSyncStatusView;
import com.srh.cinepass.entity.AutomationRun;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieSource;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.repository.AutomationRunRepository;
import com.srh.cinepass.repository.MovieRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Administrator operations on the movie catalogue.
 *
 * <p>All of these are exposed through {@code /api/admin/movies/**}, which the
 * security configuration already restricts to the ADMIN role. No new
 * authorisation path is introduced and no endpoint is left open.</p>
 *
 * <p>Nothing here ever deletes a movie, a show, a booking, a ticket, a seat
 * or a payment.</p>
 */
@Service
public class MovieAdminService {

    private static final Logger log =
            LoggerFactory.getLogger(MovieAdminService.class);

    private final MovieRepository movieRepository;
    private final AutomationRunRepository automationRunRepository;
    private final Clock clock;

    public MovieAdminService(
            MovieRepository movieRepository,
            AutomationRunRepository automationRunRepository,
            Clock clock) {

        this.movieRepository = movieRepository;
        this.automationRunRepository = automationRunRepository;
        this.clock = clock;
    }

    /**
     * Every movie, including retired ones.
     */
    @Transactional(readOnly = true)
    public List<MovieAdminView> listAllMovies() {
        return movieRepository.findAll().stream()
                .map(MovieAdminView::from)
                .toList();
    }

    /**
     * Only the movies that the automatic TMDB synchronisation manages.
     */
    @Transactional(readOnly = true)
    public List<MovieAdminView> listImportedMovies() {
        return movieRepository.findBySource(MovieSource.TMDB).stream()
                .map(MovieAdminView::from)
                .toList();
    }

    /**
     * Includes or excludes a movie from automatic show generation. The movie
     * stays visible in the customer catalogue either way.
     */
    @Transactional
    public MovieAdminView setExcluded(
            Long movieId,
            boolean excluded,
            String reason) {

        Movie movie = requireMovie(movieId);

        movie.setExcludeFromAutoShows(excluded);
        movie.setExclusionReason(excluded ? trim(reason) : null);

        movieRepository.saveAndFlush(movie);

        log.info("Movie {} automatic show generation {}", movieId, excluded ? "excluded" : "enabled");

        return MovieAdminView.from(movie);
    }

    /**
     * Protects a movie from the automatic retirement job, or removes the
     * protection.
     */
    @Transactional
    public MovieAdminView setProtected(Long movieId, boolean protectedMovie) {

        Movie movie = requireMovie(movieId);

        movie.setAdminProtected(protectedMovie);

        movieRepository.saveAndFlush(movie);

        log.info("Movie {} admin protection {}", movieId, protectedMovie ? "enabled" : "removed");

        return MovieAdminView.from(movie);
    }

    /**
     * Extends the active period of a movie. While the business date is on or
     * before {@code activeUntil} the automatic retirement job leaves the
     * movie alone.
     */
    @Transactional
    public MovieAdminView extendActivePeriod(Long movieId, LocalDate activeUntil) {

        Movie movie = requireMovie(movieId);

        movie.setActiveUntil(activeUntil);

        movieRepository.saveAndFlush(movie);

        log.info("Movie {} active period extended until {}", movieId, activeUntil);

        return MovieAdminView.from(movie);
    }

    /**
     * Brings an archived movie back into the active catalogue. The history of
     * the movie is untouched.
     */
    @Transactional
    public MovieAdminView reactivate(Long movieId) {

        Movie movie = requireMovie(movieId);

        movie.setStatus(MovieStatus.ACTIVE);
        movie.setArchivedAt(null);
        movie.setRetirementHoldReason(null);

        movieRepository.saveAndFlush(movie);

        log.info("Movie {} reactivated", movieId);

        return MovieAdminView.from(movie);
    }

    /**
     * Retires a movie manually. The movie row, its shows and all of its
     * history are preserved.
     */
    @Transactional
    public MovieAdminView archive(Long movieId) {

        Movie movie = requireMovie(movieId);

        movie.setStatus(MovieStatus.ARCHIVED);
        movie.setArchivedAt(LocalDateTime.now(clock));
        movie.setRetirementHoldReason(null);

        movieRepository.saveAndFlush(movie);

        log.info("Movie {} archived manually", movieId);

        return MovieAdminView.from(movie);
    }

    /**
     * A summary of the catalogue synchronisation state, including the TMDB
     * identifiers that are stored on more than one row. Those duplicates are
     * only reported; they are never merged or deleted automatically.
     */
    @Transactional(readOnly = true)
    public MovieSyncStatusView syncStatus() {

        List<Movie> all = movieRepository.findAll();

        long tmdbManaged = all.stream().filter(Movie::isTmdbManaged).count();
        long manual = all.size() - tmdbManaged;
        long archived = all.stream().filter(Movie::isArchived).count();
        long excluded = all.stream().filter(Movie::isExcludedFromAutoShows).count();
        long withoutTmdbId = all.stream().filter(movie -> movie.getTmdbId() == null).count();

        List<Movie> failed = all.stream()
                .filter(movie -> movie.getLastSyncError() != null
                        && !movie.getLastSyncError().isBlank())
                .toList();

        List<AutomationRun> runs = automationRunRepository.findTop50ByOrderByStartedAtDesc();

        AutomationRun lastRun = runs.isEmpty() ? null : runs.get(0);

        return new MovieSyncStatusView(
                all.size(),
                tmdbManaged,
                manual,
                archived,
                excluded,
                withoutTmdbId,
                movieRepository.findDuplicateTmdbIds(),
                failed.size(),
                failed.stream().map(MovieAdminView::from).toList(),
                lastRun == null ? null : lastRun.getStartedAt(),
                lastRun == null ? null : lastRun.getOutcome());
    }

    private Movie requireMovie(Long movieId) {

        if (movieId == null) {
            throw new IllegalArgumentException("Movie id is required");
        }

        return movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Movie not found with id: " + movieId));
    }

    private static String trim(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }
}
