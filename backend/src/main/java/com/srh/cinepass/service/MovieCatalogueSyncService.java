package com.srh.cinepass.service;

import com.srh.cinepass.dto.TmdbMovieCandidate;
import com.srh.cinepass.dto.TmdbMovieDetails;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieSource;
import com.srh.cinepass.repository.MovieRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Idempotent automatic movie catalogue synchronisation.
 *
 * <p>The flow is deliberately split in two:</p>
 * <ol>
 *   <li>This service performs the TMDB HTTP calls. It is <strong>not</strong>
 *       transactional, so no database transaction is held open while waiting
 *       for the API.</li>
 *   <li>{@link MovieCatalogueWriter} persists each movie in its own short
 *       transaction.</li>
 * </ol>
 *
 * <p>A movie only enters the catalogue when TMDB confirms an Indian
 * <em>theatrical</em> release date for it. A worldwide, digital, streaming,
 * physical or TV date never qualifies a movie, and a failed or incomplete
 * TMDB response never creates a placeholder row.</p>
 *
 * <p>The TMDB identifier is the idempotency key: an existing movie is
 * refreshed, never duplicated.</p>
 */
@Service
public class MovieCatalogueSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(MovieCatalogueSyncService.class);

    /**
     * Outcome of one synchronisation run.
     *
     * @param pagesProcessed TMDB pages that were read
     * @param discovered     movies returned by the discovery lists
     * @param inserted       movies added to the catalogue
     * @param updated        movies whose metadata was refreshed
     * @param skipped        movies that were deliberately not changed
     * @param errors         short, credential-free failure descriptions
     */
    public record SyncReport(
            int pagesProcessed,
            int discovered,
            int inserted,
            int updated,
            int skipped,
            List<String> errors) {

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }

    private final TmdbService tmdbService;
    private final MovieCatalogueWriter writer;
    private final MovieRepository movieRepository;
    private final Clock clock;

    private final boolean syncEnabled;
    private final int backfillDays;
    private final int lookaheadDays;

    public MovieCatalogueSyncService(
            TmdbService tmdbService,
            MovieCatalogueWriter writer,
            MovieRepository movieRepository,
            Clock clock,
            @Value("${app.movies.sync.enabled:true}") boolean syncEnabled,
            @Value("${app.movies.sync.backfill-days:90}") int backfillDays,
            @Value("${app.movies.sync.lookahead-days:180}") int lookaheadDays) {

        this.tmdbService = tmdbService;
        this.writer = writer;
        this.movieRepository = movieRepository;
        this.clock = clock;
        this.syncEnabled = syncEnabled;
        this.backfillDays = Math.max(0, backfillDays);
        this.lookaheadDays = Math.max(0, lookaheadDays);
    }

    /**
     * Runs one synchronisation pass.
     *
     * <p>Never throws for a single movie failure: one bad TMDB record must
     * not stop the rest of the catalogue from being synchronised.</p>
     */
    public SyncReport synchronize() {

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        List<String> errors = new ArrayList<>();

        if (!syncEnabled) {
            log.info("TMDB catalogue synchronisation is disabled");
            return new SyncReport(0, 0, 0, 0, 0, errors);
        }

        TmdbService.DiscoveryResult discovery = tmdbService.fetchDiscoveryCandidates();

        errors.addAll(discovery.errors());

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (TmdbMovieCandidate candidate : discovery.candidates()) {

            try {
                Outcome outcome = syncCandidate(candidate, today, now);

                switch (outcome) {
                    case INSERTED -> inserted++;
                    case UPDATED -> updated++;
                    default -> skipped++;
                }

            } catch (DataIntegrityViolationException duplicate) {
                /*
                 * A concurrent synchronisation inserted the same TMDB id
                 * first. The unique index did its job; this is a skip.
                 */
                skipped++;
                log.info("Skipped duplicate TMDB movie {}: already present", candidate.tmdbId());

            } catch (RuntimeException ex) {
                skipped++;
                errors.add("movie " + candidate.tmdbId() + ": " + safeMessage(ex));
                log.warn("Failed to synchronise TMDB movie {}: {}", candidate.tmdbId(), safeMessage(ex));
            }
        }

        log.info(
                "TMDB catalogue sync finished: discovered={}, inserted={}, updated={}, skipped={}",
                discovery.candidates().size(),
                inserted,
                updated,
                skipped);

        return new SyncReport(
                discovery.pagesProcessed(),
                discovery.candidates().size(),
                inserted,
                updated,
                skipped,
                errors);
    }

    private Outcome syncCandidate(
            TmdbMovieCandidate candidate,
            LocalDate today,
            LocalDateTime now) {

        Optional<TmdbMovieDetails> details =
                tmdbService.fetchMovieDetails(candidate.tmdbId());

        if (details.isEmpty()) {
            log.info(
                    "Skipped TMDB movie {}: no usable details",
                    candidate.tmdbId());
            return Outcome.SKIPPED;
        }

        TmdbMovieDetails movieDetails = details.get();

        LocalDate indiaDate = movieDetails.indiaTheatricalReleaseDate();

        if (indiaDate == null) {
            /*
             * No confirmed Indian theatrical release date. The movie is a
             * candidate only; it must not create a catalogue entry.
             */
            log.info(
                    "Skipped TMDB movie {}: no Indian theatrical release date",
                    candidate.tmdbId());
            return Outcome.SKIPPED;
        }

        Optional<Movie> existing =
                movieRepository.findByTmdbId(candidate.tmdbId());

        if (existing.isPresent()) {

            Movie movie = existing.get();

            if (movie.getSource() == MovieSource.MANUAL) {
                log.info(
                        "Skipped TMDB movie {}: manually managed, metadata preserved",
                        candidate.tmdbId());
                return Outcome.SKIPPED;
            }

            writer.updateExisting(movie.getId(), movieDetails, now);

            return Outcome.UPDATED;
        }

        if (!isInsideDiscoveryWindow(indiaDate, today)) {
            log.info(
                    "Skipped TMDB movie {}: Indian theatrical date {} is outside the discovery window",
                    candidate.tmdbId(),
                    indiaDate);
            return Outcome.SKIPPED;
        }

        writer.insertNew(movieDetails, now);

        log.info("Imported TMDB movie {} with Indian theatrical date {}", candidate.tmdbId(), indiaDate);

        return Outcome.INSERTED;
    }

    /**
     * @return true when the Indian theatrical date is close enough to today
     *         for the movie to be worth importing
     */
    private boolean isInsideDiscoveryWindow(LocalDate indiaDate, LocalDate today) {

        LocalDate earliest = today.minusDays(backfillDays);
        LocalDate latest = today.plusDays(lookaheadDays);

        return !indiaDate.isBefore(earliest) && !indiaDate.isAfter(latest);
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private enum Outcome {
        INSERTED,
        UPDATED,
        SKIPPED
    }
}
