package com.srh.cinepass.service;

import com.srh.cinepass.dto.TmdbCastMember;
import com.srh.cinepass.dto.TmdbMovieDetails;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieSource;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.repository.MovieRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transactional persistence for the automatic catalogue sync.
 *
 * <p>Every method here opens its own short transaction. The TMDB HTTP calls
 * always happen outside of these transactions, so a slow or hanging API can
 * never hold a database connection open.</p>
 *
 * <p>The unique TMDB identifier is the idempotency key. An insert that loses
 * the race against a concurrent synchronisation raises a
 * {@code DataIntegrityViolationException}; the caller treats that as a skip
 * instead of a failure.</p>
 */
@Service
public class MovieCatalogueWriter {

    private final MovieRepository movieRepository;

    public MovieCatalogueWriter(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Inserts a new TMDB-managed movie.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException when a
     *         concurrent run inserted the same TMDB identifier first
     */
    @Transactional
    public Movie insertNew(TmdbMovieDetails details, LocalDateTime now) {

        Movie movie = new Movie();

        movie.setTmdbId(details.tmdbId());
        movie.setSource(MovieSource.TMDB);
        movie.setStatus(MovieStatus.ACTIVE);

        applyMetadata(movie, details, now);

        return movieRepository.saveAndFlush(movie);
    }

    /**
     * Refreshes the metadata of an existing movie.
     *
     * @return the updated movie, or {@code null} when the id no longer exists
     */
    @Transactional
    public Movie updateExisting(
            Long movieId,
            TmdbMovieDetails details,
            LocalDateTime now) {

        Movie movie = movieRepository.findById(movieId).orElse(null);

        if (movie == null) {
            return null;
        }

        /*
         * A legacy row that already carries a TMDB id was imported from
         * TMDB, so it becomes TMDB-managed. A row an administrator marked as
         * MANUAL is never flipped here; the sync service skips it before it
         * ever reaches this method.
         */
        if (movie.getSource() == null) {
            movie.setSource(MovieSource.TMDB);
        }

        applyMetadata(movie, details, now);

        return movieRepository.saveAndFlush(movie);
    }

    /**
     * Links an existing movie to a TMDB identifier and refreshes its
     * metadata. This backs the manual "sync this movie" endpoint.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException when
     *         another movie already carries the same TMDB identifier
     */
    @Transactional
    public Movie linkTmdbMovie(
            Long movieId,
            long tmdbId,
            TmdbMovieDetails details,
            LocalDateTime now) {

        Movie movie = movieRepository.findById(movieId).orElse(null);

        if (movie == null) {
            return null;
        }

        movie.setTmdbId(tmdbId);
        movie.setSource(MovieSource.TMDB);

        applyMetadata(movie, details, now);

        return movieRepository.saveAndFlush(movie);
    }

    /**
     * Records a short, credential-free synchronisation error against an
     * existing movie so an administrator can see what went wrong.
     */
    @Transactional
    public void recordSyncError(Long movieId, String error) {

        Movie movie = movieRepository.findById(movieId).orElse(null);

        if (movie == null) {
            return;
        }

        movie.setLastSyncError(truncate(error));

        movieRepository.save(movie);
    }

    /**
     * Copies the TMDB metadata onto the movie.
     *
     * <p>Only TMDB-managed metadata is touched. Administrator overrides
     * (exclusion, protection, active period, lifecycle state) are never
     * overwritten, and a release date that an administrator set by hand is
     * preserved.</p>
     */
    private void applyMetadata(
            Movie movie,
            TmdbMovieDetails details,
            LocalDateTime now) {

        movie.setTitle(details.title());

        if (details.overview() != null && !details.overview().isBlank()) {
            movie.setDescription(details.overview());
        }

        if (details.posterUrl() != null && !details.posterUrl().isBlank()) {
            movie.setPosterUrl(details.posterUrl());
        }

        if (details.backdropUrl() != null && !details.backdropUrl().isBlank()) {
            movie.setBackdropUrl(details.backdropUrl());
        }

        if (details.originalLanguage() != null
                && !details.originalLanguage().isBlank()) {
            movie.setLanguage(details.originalLanguage());
        }

        if (details.genres() != null && !details.genres().isBlank()) {
            movie.setGenre(details.genres());
        }

        if (details.rating() != null && details.rating() > 0) {
            movie.setRating(details.rating());
        }

        if (details.runtime() != null && details.runtime() > 0) {
            movie.setDuration(details.runtime());
        }

        // Trailer key: only overwrite when TMDB provides one, or when the
        // movie has none yet. This avoids clearing an existing trailer on a
        // failed fetch.
        if (details.trailerKey() != null && !details.trailerKey().isBlank()) {
            movie.setTrailerKey(details.trailerKey());
        } else if (movie.getTrailerKey() == null) {
            movie.setTrailerKey(null);
        }

        // Cast: only overwrite when TMDB provides a non-empty list, or when
        // the movie has none yet.
        if (details.cast() != null && !details.cast().isEmpty()) {
            movie.setCastJson(toCastJson(details.cast()));
        } else if (movie.getCastJson() == null) {
            movie.setCastJson(null);
        }

        LocalDate indiaDate = details.indiaTheatricalReleaseDate();

        if (indiaDate != null) {

            LocalDate previousIndiaDate = movie.getIndiaTheatricalReleaseDate();
            LocalDate previousReleaseDate = movie.getReleaseDate();

            movie.setIndiaTheatricalReleaseDate(indiaDate);

            /*
             * Only refresh releaseDate when it was derived from TMDB before
             * (equal to the previous Indian date) or when it was never set.
             * A hand-picked release date stays untouched.
             */
            boolean tmdbDerived = previousIndiaDate != null
                    && previousIndiaDate.equals(previousReleaseDate);

            if (previousReleaseDate == null || tmdbDerived) {
                movie.setReleaseDate(indiaDate);
            }
        }

        movie.setTmdbSyncedAt(now);
        movie.setLastSyncError(null);
    }

    /**
     * Serialises the cast list to a compact JSON array. Only the fields the
     * frontend needs are included; profile paths are stored as TMDB relative
     * paths so the frontend can build the image URL.
     */
    private static String toCastJson(List<TmdbCastMember> cast) {

        if (cast == null || cast.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < cast.size(); i++) {

            if (i > 0) {
                sb.append(",");
            }

            TmdbCastMember member = cast.get(i);

            sb.append("{\"name\":\"").append(escapeJson(member.name())).append("\",")
                    .append("\"character\":\"").append(escapeJson(member.character())).append("\",")
                    .append("\"profilePath\":\"").append(escapeJson(member.profilePath())).append("\"}");
        }

        sb.append("]");

        return sb.toString();
    }

    private static String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String truncate(String value) {

        if (value == null) {
            return null;
        }

        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
