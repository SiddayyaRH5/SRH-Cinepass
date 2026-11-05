package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Booking;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> origin/main

import java.util.List;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

<<<<<<< HEAD
    @Override
    @EntityGraph(attributePaths = {"show", "show.movie", "show.theatre", "show.theatre.location", "show.theatre.owner", "seatNumbers"})
    List<Booking> findAll();

    @Override
    @EntityGraph(attributePaths = {"show", "show.movie", "show.theatre", "show.theatre.location", "show.theatre.owner", "seatNumbers"})
    java.util.Optional<Booking> findById(Long id);

    @EntityGraph(attributePaths = {"show", "show.movie", "show.theatre", "show.theatre.location", "show.theatre.owner", "seatNumbers"})
    List<Booking> findByUserId(String userId);

    @EntityGraph(attributePaths = {"show", "show.movie", "show.theatre", "show.theatre.location", "show.theatre.owner", "seatNumbers"})
    List<Booking> findByUserEmail(String userEmail);

    @EntityGraph(attributePaths = {"show", "show.movie", "show.theatre", "show.theatre.location", "show.theatre.owner", "seatNumbers"})
=======
    List<Booking> findByUserId(String userId);

    List<Booking> findByUserEmail(String userEmail);

>>>>>>> origin/main
    List<Booking> findByShowId(Long showId);
}