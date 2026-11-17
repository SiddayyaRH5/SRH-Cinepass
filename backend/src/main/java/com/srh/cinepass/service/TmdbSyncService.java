package com.srh.cinepass.service;

import com.srh.cinepass.dto.TmdbCastMember;
import com.srh.cinepass.dto.TmdbMovieDetails;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.repository.MovieRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The manual "sync this movie with TMDB" service behind
 * {@code POST /api/tmdb/sync/{movieId}}.
 *
 * <p>The TMDB HTTP call happens outside of any transaction, and the
 * persistence happens in a short transaction of its own. That keeps the
 * database connection free while the API is contacted.</p>
 *
 * <p>The release date is taken from the confirmed Indian theatrical release
 * date only. A worldwide, digital, streaming, physical or TV date is never
 * used as the Indian theatrical release date. When TMDB has no Indian
 * theatrical date for the movie, the metadata is still imported but the
 * release date is left untouched and the reason is recorded on the movie, so
 * the scheduler never schedules the movie before an Indian date is known.
 */
@Service
public class TmdbSyncService {

    private final TmdbService tmdbService;
    private final MovieRepository movieRepository;
    private final MovieCatalogueWriter writer;
    private final Clock clock;

    public TmdbSyncService(
            TmdbService tmdbService,
            MovieRepository movieRepository,
            MovieCatalogueWriter writer,
            Clock clock) {

        this.tmdbService = tmdbService;
        this.movieRepository = movieRepository;
        this.writer = writer;
        this.clock = clock;
    }

    public Movie syncMovie(Long movieId, Long tmdbId) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));

        if (tmdbId == null || tmdbId <= 0) {
            throw new RuntimeException("A valid TMDB movie id is required");
        }

        Optional<TmdbMovieDetails> details = tmdbService.fetchMovieDetails(tmdbId);

        if (details.isEmpty()) {
            writer.recordSyncError(movieId,
                    "TMDB has no usable record for movie id " + tmdbId);

            throw new RuntimeException(
                    "TMDB returned no usable movie for id: " + tmdbId);
        }

        TmdbMovieDetails baseDetails = details.get();

        // Fetch credits and trailer independently so a failure in one does
        // not prevent the other from being saved.
        List<TmdbCastMember> cast = tmdbService.fetchCredits(tmdbId);
        String trailerKey = tmdbService.fetchTrailerKey(tmdbId);

        TmdbMovieDetails movieDetails = new TmdbMovieDetails(
                baseDetails.tmdbId(),
                baseDetails.title(),
                baseDetails.overview(),
                baseDetails.posterUrl(),
                baseDetails.backdropUrl(),
                baseDetails.originalLanguage(),
                baseDetails.genres(),
                baseDetails.rating(),
                baseDetails.runtime(),
                baseDetails.indiaTheatricalReleaseDate(),
                trailerKey,
                cast);

        LocalDateTime now = LocalDateTime.now(clock);

        try {
            Movie updated = writer.linkTmdbMovie(movieId, tmdbId, movieDetails, now);

            if (updated == null) {
                throw new RuntimeException("Movie not found: " + movieId);
            }

            if (movieDetails.indiaTheatricalReleaseDate() == null) {
                writer.recordSyncError(movieId,
                        "TMDB has no Indian theatrical release date; "
                        + "the release date was left unchanged");
            }

            return updated;

        } catch (DataIntegrityViolationException duplicate) {
            writer.recordSyncError(movieId,
                    "Another movie already uses TMDB id " + tmdbId);

            throw new RuntimeException(
                    "TMDB id " + tmdbId + " is already linked to another movie");
        }
    }
}
