package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowId(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Seat> findById(Long id);
}