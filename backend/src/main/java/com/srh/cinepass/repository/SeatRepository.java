package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Seat;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> origin/main
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

<<<<<<< HEAD
    @EntityGraph(attributePaths = {"show", "show.movie", "show.theatre", "show.theatre.location", "show.theatre.owner"})
=======
>>>>>>> origin/main
    List<Seat> findByShowId(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Seat> findById(Long id);

    void deleteByShowId(Long showId);
}