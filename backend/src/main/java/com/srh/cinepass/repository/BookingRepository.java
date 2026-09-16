package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByUserEmail(String userEmail);

    List<Booking> findByShowId(Long showId);
}