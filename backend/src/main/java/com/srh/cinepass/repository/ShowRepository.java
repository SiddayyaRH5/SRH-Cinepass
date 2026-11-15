package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.entity.Theatre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieId(Long movieId);

    List<Show> findByTheatreId(Long theatreId);

    /*
     * Shows store a single IST wall-clock instant. Date queries therefore use
     * a half-open [from, to) range instead of a non-persistent "showDate"
     * property, which keeps them correct on both MySQL and H2.
     */

    List<Show> findByShowTimeGreaterThanEqualAndShowTimeLessThanOrderByShowTimeAsc(
            LocalDateTime from,
            LocalDateTime to);

    List<Show> findByMovieIdAndShowTimeGreaterThanEqualAndShowTimeLessThanOrderByShowTimeAsc(
            Long movieId,
            LocalDateTime from,
            LocalDateTime to);

    List<Show> findByTheatreIdAndShowTimeGreaterThanEqualAndShowTimeLessThanOrderByShowTimeAsc(
            Long theatreId,
            LocalDateTime from,
            LocalDateTime to);

    /*
     * The uniqueness key includes the exact scheduled instant, so a theatre
     * may run the same movie several times a day but never twice at the same
     * time. This is the check the scheduler and manual creation share.
     */
    boolean existsByMovieIdAndTheatreIdAndShowTime(
            Long movieId,
            Long theatreId,
            LocalDateTime showTime);

    boolean existsByMovieIdAndTheatreIdAndShowTimeAndIdNot(
            Long movieId,
            Long theatreId,
            LocalDateTime showTime,
            Long id);

    /*
     * A theatre is one screen in this model, so the scheduler must not place
     * two different movies at the same theatre and instant. This is broader
     * than the (movie, theatre, time) uniqueness key and is only used by the
     * scheduler to avoid overlapping slots.
     */
    boolean existsByTheatreIdAndShowTime(Long theatreId, LocalDateTime showTime);

    // ------------------------------------------------------------
    // Movie-based discovery (database-side filtering)
    // ------------------------------------------------------------

    /*
     * Shared eligibility predicate: only SCHEDULED (or legacy null) shows,
     * starting after the bookable cut-off, at a theatre that has a real seat
     * count, a location, and either no owner (admin-created) or an APPROVED
     * owner verification. Archived movies are excluded so a retired movie is
     * never offered to a customer as bookable. All filtering stays in the
     * database.
     */
    String AVAILABLE_SHOW_PREDICATE = """
              AND s.showTime > :from
              AND (s.status IS NULL OR s.status = :scheduled)
              AND (s.movie.status IS NULL OR s.movie.status = :active)
              AND s.theatre.totalSeats > 0
              AND s.theatre.location IS NOT NULL
              AND (s.theatre.owner IS NULL OR EXISTS (
                    SELECT v.id FROM TheatreVerification v
                    WHERE v.user = s.theatre.owner
                      AND v.status = :approved
              ))
            """;

    @Query("""
            SELECT DISTINCT s.theatre.location
            FROM Show s
            WHERE s.movie.id = :movieId
            """ + AVAILABLE_SHOW_PREDICATE + """
            ORDER BY s.theatre.location.city ASC
            """)
    List<Location> findCitiesWithAvailableShows(
            @Param("movieId") Long movieId,
            @Param("from") LocalDateTime from,
            @Param("scheduled") ShowStatus scheduled,
            @Param("approved") String approved,
            @Param("active") MovieStatus active);

    @Query("""
            SELECT DISTINCT s.theatre
            FROM Show s
            WHERE s.movie.id = :movieId
              AND s.theatre.location.id = :locationId
            """ + AVAILABLE_SHOW_PREDICATE + """
            ORDER BY s.theatre.name ASC
            """)
    List<Theatre> findTheatresWithAvailableShows(
            @Param("movieId") Long movieId,
            @Param("locationId") Long locationId,
            @Param("from") LocalDateTime from,
            @Param("scheduled") ShowStatus scheduled,
            @Param("approved") String approved,
            @Param("active") MovieStatus active);

    @Query("""
            SELECT s FROM Show s
            WHERE s.movie.id = :movieId
              AND s.theatre.id = :theatreId
            """ + AVAILABLE_SHOW_PREDICATE + """
              AND s.showTime < :to
            ORDER BY s.showTime ASC
            """)
    List<Show> findAvailableShows(
            @Param("movieId") Long movieId,
            @Param("theatreId") Long theatreId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("scheduled") ShowStatus scheduled,
            @Param("approved") String approved,
            @Param("active") MovieStatus active);

    /**
     * Distinct calendar dates (IST) that still have bookable shows for the
     * movie/theatre pair. Date arithmetic is done in Java from the returned
     * instants, so the query stays portable across MySQL and H2.
     */
    @Query("""
            SELECT DISTINCT s.showTime
            FROM Show s
            WHERE s.movie.id = :movieId
              AND s.theatre.id = :theatreId
            """ + AVAILABLE_SHOW_PREDICATE + """
            ORDER BY s.showTime ASC
            """)
    List<LocalDateTime> findAvailableShowInstants(
            @Param("movieId") Long movieId,
            @Param("theatreId") Long theatreId,
            @Param("from") LocalDateTime from,
            @Param("scheduled") ShowStatus scheduled,
            @Param("approved") String approved,
            @Param("active") MovieStatus active);

    // ------------------------------------------------------------
    // Movie lifecycle support
    // ------------------------------------------------------------

    /**
     * Counts shows of a movie that are still scheduled to start in the
     * future. A movie with such a show must not be retired, because the
     * show still has to be honoured.
     */
    @Query("""
            SELECT COUNT(s) FROM Show s
            WHERE s.movie.id = :movieId
              AND s.showTime > :from
              AND (s.status IS NULL OR s.status = :scheduled)
            """)
    long countFutureScheduledShows(
            @Param("movieId") Long movieId,
            @Param("from") LocalDateTime from,
            @Param("scheduled") ShowStatus scheduled);

    // ------------------------------------------------------------
    // Lifecycle cleanup
    // ------------------------------------------------------------

    /**
     * Marks only shows that are still SCHEDULED (or legacy null) as EXPIRED.
     * Bookings, tickets, seats and payment rows are untouched. Re-running the
     * statement is a no-op because already-expired rows are excluded.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Show s
            SET s.status = :expired
            WHERE s.showTime <= :threshold
              AND (s.status IS NULL OR s.status = :scheduled)
            """)
    int markExpired(
            @Param("threshold") LocalDateTime threshold,
            @Param("expired") ShowStatus expired,
            @Param("scheduled") ShowStatus scheduled);
}
