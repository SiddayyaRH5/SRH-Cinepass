package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieSource;
import com.srh.cinepass.entity.MovieStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    /**
     * Movies the scheduler may create shows for.
     *
     * <p>A movie qualifies when it has a real title, is still active, has not
     * been excluded by an administrator, and has already reached its Indian
     * theatrical release date.</p>
     *
     * <p>A TMDB-managed movie additionally needs a <em>confirmed</em> Indian
     * theatrical date, so a movie whose Indian date is still unknown can
     * never reach a screen. Manually managed movies keep the legacy
     * behaviour: they are eligible as soon as their release date is reached
     * or when they have no release date at all.</p>
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.title IS NOT NULL
              AND TRIM(m.title) <> ''
              AND (m.status IS NULL OR m.status = :active)
              AND (m.excludeFromAutoShows IS NULL OR m.excludeFromAutoShows = false)
              AND (m.indiaTheatricalReleaseDate IS NULL OR m.indiaTheatricalReleaseDate <= :today)
              AND (m.releaseDate IS NULL OR m.releaseDate <= :today)
              AND (
                    (m.source IS NULL OR m.source <> :tmdb)
                    OR (m.indiaTheatricalReleaseDate IS NOT NULL
                        AND m.indiaTheatricalReleaseDate <= :today)
              )
            ORDER BY m.id ASC
            """)
    List<Movie> findEligibleForShows(
            @Param("today") LocalDate today,
            @Param("active") MovieStatus active,
            @Param("tmdb") MovieSource tmdb);

    /**
     * Convenience overload that keeps the previous single-argument contract.
     */
    default List<Movie> findEligibleForShows(LocalDate today) {
        return findEligibleForShows(today, MovieStatus.ACTIVE, MovieSource.TMDB);
    }

    /**
     * The customer-facing catalogue: only movies that have not been retired.
     * Archived movies keep their row, their shows and their history but are
     * not offered to customers.
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.status IS NULL OR m.status = :active
            ORDER BY m.id ASC
            """)
    List<Movie> findActiveMovies(@Param("active") MovieStatus active);

    /**
     * Every movie that is still in the active lifecycle state, used by the
     * retirement job.
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.status IS NULL OR m.status = :active
            ORDER BY m.id ASC
            """)
    List<Movie> findActiveMoviesForLifecycle(@Param("active") MovieStatus active);

    /**
     * Movies imported or refreshed by the automatic TMDB synchronisation.
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.source = :source
            ORDER BY m.id ASC
            """)
    List<Movie> findBySource(@Param("source") MovieSource source);

    /**
     * Looks a movie up by its stable TMDB identifier. This is the key that
     * makes the catalogue sync idempotent.
     */
    Optional<Movie> findByTmdbId(Long tmdbId);

    boolean existsByTmdbId(Long tmdbId);

    /**
     * TMDB identifiers that are stored on more than one movie row. Reported
     * to the administrator; the rows are never merged or deleted automatically.
     */
    @Query("""
            SELECT m.tmdbId FROM Movie m
            WHERE m.tmdbId IS NOT NULL
            GROUP BY m.tmdbId
            HAVING COUNT(m) > 1
            """)
    List<Long> findDuplicateTmdbIds();

    /**
     * Movie rows that carry no TMDB identifier at all. These are manual or
     * legacy rows and are reported, never touched.
     */
    long countByTmdbIdIsNull();
}
