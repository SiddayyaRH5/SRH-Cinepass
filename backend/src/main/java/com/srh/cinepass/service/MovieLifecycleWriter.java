package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.repository.MovieRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transactional persistence for the movie lifecycle job.
 *
 * <p>Each movie is handled in its own short transaction so one problem never
 * rolls back the whole retirement pass. Nothing here ever deletes a movie,
 * a show, a booking, a ticket, a seat or a payment.</p>
 */
@Service
public class MovieLifecycleWriter {

    private final MovieRepository movieRepository;

    public MovieLifecycleWriter(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Retires a movie. The row and all of its history stay in place; only the
     * lifecycle state changes.
     */
    @Transactional
    public Movie archive(Long movieId, LocalDateTime now) {

        Movie movie = movieRepository.findById(movieId).orElse(null);

        if (movie == null) {
            return null;
        }

        movie.setStatus(MovieStatus.ARCHIVED);
        movie.setArchivedAt(now);
        movie.setRetirementHoldReason(null);

        return movieRepository.saveAndFlush(movie);
    }

    /**
     * Records why the automatic retirement of a movie was postponed.
     */
    @Transactional
    public void recordHold(Long movieId, String reason) {

        Movie movie = movieRepository.findById(movieId).orElse(null);

        if (movie == null) {
            return;
        }

        movie.setRetirementHoldReason(reason);

        movieRepository.save(movie);
    }
}
