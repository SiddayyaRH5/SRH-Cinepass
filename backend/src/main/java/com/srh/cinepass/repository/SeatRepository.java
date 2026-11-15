
package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Seat;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // Get all seats belonging to a show.
    List<Seat> findByShowId(Long showId);

    // Get seats in a consistent order for the seat layout.
    List<Seat> findByShowIdOrderByIdAsc(Long showId);

    // Lock a seat row while a booking transaction is in progress.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdForUpdate(@Param("seatId") Long seatId);

    // Get already-booked seats for a show.
    List<Seat> findByShowIdAndBookedTrue(Long showId);

    // Used to guarantee seat generation never runs twice for the same show.
    long countByShowId(Long showId);

    /*
     * Availability aggregate for a page of shows. One row per show:
     * [showId, totalSeats, unavailableSeats]. A seat counts as unavailable
     * when it is permanently booked or has an active temporary reservation.
     */
    @Query("""
            SELECT s.show.id,
                   COUNT(s),
                   SUM(CASE WHEN s.booked = true
                             OR (s.reservedUntil IS NOT NULL AND s.reservedUntil > :now)
                        THEN 1 ELSE 0 END)
            FROM Seat s
            WHERE s.show.id IN :showIds
            GROUP BY s.show.id
            """)
    List<Object[]> countAvailability(
            @Param("showIds") List<Long> showIds,
            @Param("now") LocalDateTime now);

    // Delete seats belonging to a show.
    void deleteByShowId(Long showId);
}
