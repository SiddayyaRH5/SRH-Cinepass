package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.repository.SeatRepository;
import com.srh.cinepass.repository.ShowRepository;
import com.srh.cinepass.repository.TheatreRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;
    private final OwnerAuthorizationService ownerAuthorizationService;

    public ShowService(
            ShowRepository showRepository,
            MovieRepository movieRepository,
            TheatreRepository theatreRepository,
            SeatRepository seatRepository,
            OwnerAuthorizationService ownerAuthorizationService) {

        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.seatRepository = seatRepository;
        this.ownerAuthorizationService = ownerAuthorizationService;
    }

    // ============================================================
    // GET ALL SHOWS
    // ============================================================

    public List<Show> getAllShows() {
        return showRepository.findAll();
    }

    // ============================================================
    // GET SHOW BY ID
    // ============================================================

    public Show getShowById(Long id) {

        return showRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Show not found"));
    }

    // ============================================================
    // GET SHOWS BY MOVIE
    // ============================================================

    public List<Show> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId);
    }

    // ============================================================
    // GET SHOWS BY THEATRE
    // ============================================================

    public List<Show> getShowsByTheatre(Long theatreId) {
        return showRepository.findByTheatreId(theatreId);
    }

    // ============================================================
    // GET SHOWS BY DATE
    // ============================================================

    public List<Show> getShowsByDate(LocalDate date) {
        return showRepository.findByShowDate(date);
    }

    // ============================================================
    // GET SHOWS BY MOVIE + DATE
    // ============================================================

    public List<Show> getShowsByMovieAndDate(
            Long movieId,
            LocalDate date) {

        return showRepository.findByMovieIdAndShowDate(
                movieId,
                date
        );
    }

    // ============================================================
    // GET SHOWS BY THEATRE + DATE
    // ============================================================

    public List<Show> getShowsByTheatreAndDate(
            Long theatreId,
            LocalDate date) {

        return showRepository.findByTheatreIdAndShowDate(
                theatreId,
                date
        );
    }

    // ============================================================
    // ADD SHOW
    // ============================================================

    @Transactional
    public Show addShow(
            Show show,
            Long userId,
            boolean isAdmin) {

        if (show == null) {
            throw new RuntimeException("Show data is required");
        }

        if (show.getMovie() == null ||
                show.getMovie().getId() == null) {

            throw new RuntimeException("Movie is required");
        }

        if (show.getTheatre() == null ||
                show.getTheatre().getId() == null) {

            throw new RuntimeException("Theatre is required");
        }

        Long movieId = show.getMovie().getId();
        Long theatreId = show.getTheatre().getId();

        checkPermission(
                userId,
                theatreId,
                isAdmin
        );

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() ->
                        new RuntimeException("Movie not found"));

        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() ->
                        new RuntimeException("Theatre not found"));

        if (show.getShowDate() == null) {
            throw new RuntimeException("Show date is required");
        }

        if (show.getShowTime() == null) {
            throw new RuntimeException("Show time is required");
        }

        if (show.getTicketPrice() == null) {
            throw new RuntimeException("Ticket price is required");
        }

        if (show.getTicketPrice() < 0) {
            throw new RuntimeException(
                    "Ticket price cannot be negative"
            );
        }

        boolean alreadyExists =
                showRepository
                        .existsByMovieIdAndTheatreIdAndShowDateAndShowTime(
                                movieId,
                                theatreId,
                                show.getShowDate(),
                                show.getShowTime()
                        );

        if (alreadyExists) {
            throw new RuntimeException(
                    "A show already exists for this movie, theatre, date and time"
            );
        }

        show.setMovie(movie);
        show.setTheatre(theatre);

        Show savedShow = showRepository.save(show);

        // Create seats automatically
        createSeatsForShow(savedShow, theatre.getTotalSeats());

        return savedShow;
    }

    // ============================================================
    // CREATE SEATS
    // ============================================================

    private void createSeatsForShow(
            Show show,
            int totalSeats) {

        if (totalSeats <= 0) {
            throw new RuntimeException(
                    "Theatre must have at least one seat"
            );
        }

        for (int i = 1; i <= totalSeats; i++) {

            Seat seat = new Seat();

            seat.setSeatNumber("S" + i);
            seat.setBooked(false);
            seat.setShow(show);

            seatRepository.save(seat);
        }
    }

    // ============================================================
    // UPDATE SHOW
    // ============================================================

    @Transactional
    public Show updateShow(
            Long showId,
            Long movieId,
            Long theatreId,
            Show updatedShow,
            Long userId,
            boolean isAdmin) {

        Show existingShow = showRepository.findById(showId)
                .orElseThrow(() ->
                        new RuntimeException("Show not found"));

        if (existingShow.getTheatre() == null ||
                existingShow.getTheatre().getId() == null) {

            throw new RuntimeException(
                    "Existing show has no theatre"
            );
        }

        Long existingTheatreId =
                existingShow.getTheatre().getId();

        // Existing theatre must be manageable
        checkPermission(
                userId,
                existingTheatreId,
                isAdmin
        );

        // If theatre is changed, new theatre must also
        // belong to the logged-in owner.
        if (theatreId != null &&
                !theatreId.equals(existingTheatreId)) {

            checkPermission(
                    userId,
                    theatreId,
                    isAdmin
            );
        }

        if (movieId == null) {
            throw new RuntimeException("Movie is required");
        }

        if (theatreId == null) {
            theatreId = existingTheatreId;
        }

        if (updatedShow.getShowDate() == null) {
            throw new RuntimeException("Show date is required");
        }

        if (updatedShow.getShowTime() == null) {
            throw new RuntimeException("Show time is required");
        }

        if (updatedShow.getTicketPrice() == null) {
            throw new RuntimeException("Ticket price is required");
        }

        if (updatedShow.getTicketPrice() < 0) {
            throw new RuntimeException(
                    "Ticket price cannot be negative"
            );
        }

        boolean alreadyExists =
                showRepository
                        .existsByMovieIdAndTheatreIdAndShowDateAndShowTimeAndIdNot(
                                movieId,
                                theatreId,
                                updatedShow.getShowDate(),
                                updatedShow.getShowTime(),
                                showId
                        );

        if (alreadyExists) {
            throw new RuntimeException(
                    "Another show already exists for this movie, theatre, date and time"
            );
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() ->
                        new RuntimeException("Movie not found"));

        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() ->
                        new RuntimeException("Theatre not found"));

        existingShow.setMovie(movie);
        existingShow.setTheatre(theatre);
        existingShow.setShowDate(
                updatedShow.getShowDate()
        );
        existingShow.setShowTime(
                updatedShow.getShowTime()
        );
        existingShow.setTicketPrice(
                updatedShow.getTicketPrice()
        );

        return showRepository.save(existingShow);
    }

    // ============================================================
    // DELETE SHOW
    // ============================================================

    @Transactional
    public void deleteShow(
            Long showId,
            Long userId,
            boolean isAdmin) {

        Show existingShow = showRepository.findById(showId)
                .orElseThrow(() ->
                        new RuntimeException("Show not found"));

        if (existingShow.getTheatre() == null ||
                existingShow.getTheatre().getId() == null) {

            throw new RuntimeException(
                    "Show has no theatre"
            );
        }

        Long theatreId =
                existingShow.getTheatre().getId();

        checkPermission(
                userId,
                theatreId,
                isAdmin
        );

        // Delete seats belonging to this show first
        seatRepository.deleteByShowId(showId);

        // Then delete show
        showRepository.delete(existingShow);
    }

    // ============================================================
    // GET SEATS FOR SHOW
    // ============================================================

    public List<Seat> getSeatsByShow(Long showId) {

        if (!showRepository.existsById(showId)) {
            throw new RuntimeException(
                    "Show not found"
            );
        }

        return seatRepository.findByShowId(showId);
    }

    // ============================================================
    // PERMISSION CHECK
    // ============================================================

    private void checkPermission(
            Long userId,
            Long theatreId,
            boolean isAdmin) {

        // ADMIN can manage every theatre
        if (isAdmin) {
            return;
        }

        if (userId == null) {
            throw new RuntimeException(
                    "Authentication required"
            );
        }

        // Theatre owner must be an approved owner
        // and must own this particular theatre.
        if (!ownerAuthorizationService
                .canManageTheatre(userId, theatreId)) {

            throw new RuntimeException(
                    "You are not authorized to manage this theatre"
            );
        }
    }
}