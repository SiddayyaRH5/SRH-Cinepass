package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Show;

import org.springframework.data.jpa.repository.JpaRepository;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
>>>>>>> origin/main

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

<<<<<<< HEAD
    @Override
    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
    List<Show> findAll();

    @Override
    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
    java.util.Optional<Show> findById(Long id);

    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
    List<Show> findByMovieId(Long movieId);

    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
    List<Show> findByTheatreId(Long theatreId);

    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
    List<Show> findByShowDate(LocalDate showDate);

    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
=======
    List<Show> findByMovieId(Long movieId);

    List<Show> findByTheatreId(Long theatreId);

    List<Show> findByShowDate(LocalDate showDate);

>>>>>>> origin/main
    List<Show> findByMovieIdAndShowDate(
            Long movieId,
            LocalDate showDate
    );

<<<<<<< HEAD
    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
=======
>>>>>>> origin/main
    List<Show> findByTheatreIdAndShowDate(
            Long theatreId,
            LocalDate showDate
    );

    boolean existsByMovieIdAndTheatreIdAndShowDateAndShowTime(
            Long movieId,
            Long theatreId,
            LocalDate showDate,
            LocalTime showTime
    );

    boolean existsByMovieIdAndTheatreIdAndShowDateAndShowTimeAndIdNot(
            Long movieId,
            Long theatreId,
            LocalDate showDate,
            LocalTime showTime,
            Long id
    );
}