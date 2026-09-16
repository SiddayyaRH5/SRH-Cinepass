package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieId(Long movieId);

    List<Show> findByTheatreId(Long theatreId);

    List<Show> findByShowDate(LocalDate showDate);

    List<Show> findByMovieIdAndShowDate(
            Long movieId,
            LocalDate showDate
    );

    // Get shows for a theatre on a particular date
    List<Show> findByTheatreIdAndShowDate(
            Long theatreId,
            LocalDate showDate
    );
}