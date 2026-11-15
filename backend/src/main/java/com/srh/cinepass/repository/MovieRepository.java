package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Movie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    /**
     * Movies the scheduler may create shows for: a real title, and either no
     * release date (legacy/manual entries) or one that is not in the future.
     */
    @Query("""
            SELECT m FROM Movie m
            WHERE m.title IS NOT NULL
              AND TRIM(m.title) <> ''
              AND (m.releaseDate IS NULL OR m.releaseDate <= :today)
            ORDER BY m.id ASC
            """)
    List<Movie> findEligibleForShows(@Param("today") LocalDate today);
}
