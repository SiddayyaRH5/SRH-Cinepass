
package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Seat;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.QueryHints;

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

    // Delete seats belonging to a show.
    void deleteByShowId(Long showId);
}
