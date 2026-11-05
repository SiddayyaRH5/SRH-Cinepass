package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Show;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

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
    List<Show> findByMovieIdAndShowDate(
            Long movieId,
            LocalDate showDate
    );

    @EntityGraph(attributePaths = {"movie", "theatre", "theatre.location", "theatre.owner"})
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