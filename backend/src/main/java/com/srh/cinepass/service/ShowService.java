package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.repository.BookingRepository;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.repository.SeatRepository;
import com.srh.cinepass.repository.ShowRepository;
import com.srh.cinepass.repository.TheatreRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final OwnerAuthorizationService ownerAuthorizationService;

    public ShowService(
            ShowRepository showRepository,
            MovieRepository movieRepository,
            TheatreRepository theatreRepository,
            SeatRepository seatRepository,
            BookingRepository bookingRepository,
            OwnerAuthorizationService ownerAuthorizationService) {

        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
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
    //
    // Shows are stored as a single IST wall-clock instant, so a calendar day is
    // the half-open range [date 00:00, date+1 00:00). This replaces the old
    // derived "showDate" query, which was not a persisted property.
    // ============================================================

    public List<Show> getShowsByDate(LocalDate date) {
        return showRepository
                .findByShowTimeGreaterThanEqualAndShowTimeLessThanOrderByShowTimeAsc(
                        startOf(date),
                        startOf(date.plusDays(1))
                );
    }

    // ============================================================
    // GET SHOWS BY MOVIE + DATE
    // ============================================================

    public List<Show> getShowsByMovieAndDate(
            Long movieId,
            LocalDate date) {

        return showRepository
                .findByMovieIdAndShowTimeGreaterThanEqualAndShowTimeLessThanOrderByShowTimeAsc(
                        movieId,
                        startOf(date),
                        startOf(date.plusDays(1))
                );
    }

    // ============================================================
    // GET SHOWS BY THEATRE + DATE
    // ============================================================

    public List<Show> getShowsByTheatreAndDate(
            Long theatreId,
            LocalDate date) {

        return showRepository
                .findByTheatreIdAndShowTimeGreaterThanEqualAndShowTimeLessThanOrderByShowTimeAsc(
                        theatreId,
                        startOf(date),
                        startOf(date.plusDays(1))
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

        LocalDateTime showDateTime = show.getShowDateTime();

        if (showDateTime == null) {
            throw new RuntimeException(
                    "Show date and time are required"
            );
        }

        /*
         * Uniqueness is the exact scheduled instant, not movie+theatre+date.
         * That still allows several showtimes for the same movie on one day.
         */
        boolean alreadyExists =
                showRepository
                        .existsByMovieIdAndTheatreIdAndShowTime(
                                movieId,
                                theatreId,
                                showDateTime
                        );

        if (alreadyExists) {
            throw new RuntimeException(
                    "A show already exists for this movie, theatre and time"
            );
        }

        show.setMovie(movie);
        show.setTheatre(theatre);

        // Legacy rows may have a null status; new rows always start SCHEDULED.
        show.setStatus(ShowStatus.SCHEDULED);

        Show savedShow = showRepository.save(show);

        // Create seats automatically (idempotent guard inside)
        createSeatsForShow(savedShow, theatre.getTotalSeats());

        return savedShow;
    }

    // ============================================================
    // SCHEDULER-FACING SHOW CREATION
    //
    // Each show runs in its own transaction so a single constraint violation or
    // bad theatre cannot roll back an entire daily generation run. The method is
    // idempotent: an existing show for the exact instant returns null.
    // ============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Show addShowForScheduler(
            Movie movie,
            Theatre theatre,
            LocalDateTime showTime,
            Double ticketPrice) {

        if (movie == null || movie.getId() == null) {
            throw new RuntimeException("Movie is required");
        }

        if (theatre == null || theatre.getId() == null) {
            throw new RuntimeException("Theatre is required");
        }

        if (showTime == null) {
            throw new RuntimeException("Show time is required");
        }

        if (ticketPrice == null || ticketPrice < 0) {
            throw new RuntimeException(
                    "Ticket price must be zero or greater"
            );
        }

        boolean alreadyExists =
                showRepository
                        .existsByMovieIdAndTheatreIdAndShowTime(
                                movie.getId(),
                                theatre.getId(),
                                showTime
                        );

        if (alreadyExists) {
            return null;
        }

        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowTime(showTime);
        show.setTicketPrice(ticketPrice);
        show.setStatus(ShowStatus.SCHEDULED);

        /*
         * Flush so a concurrent insert that slipped past the exists() check
         * fails inside this transaction (caught by the generation service)
         * instead of at commit time, where it would be harder to attribute.
         */
        Show savedShow = showRepository.saveAndFlush(show);

        createSeatsForShow(savedShow, theatre.getTotalSeats());

        return savedShow;
    }

    // ============================================================
    // CREATE SEATS
    //
    // Idempotent: if the show already has any seat rows they are left exactly
    // as they are. Booked and temporarily reserved seats are therefore never
    // touched or regenerated.
    // ============================================================

    @Transactional
    public void createSeatsForShow(
            Show show,
            int totalSeats) {

        if (show == null || show.getId() == null) {
            throw new RuntimeException("Show is required");
        }

        if (totalSeats <= 0) {
            throw new RuntimeException(
                    "Theatre must have at least one seat"
            );
        }

        if (seatRepository.countByShowId(show.getId()) > 0) {
            return;
        }

        List<Seat> seats = new ArrayList<>(totalSeats);

        for (int i = 1; i <= totalSeats; i++) {

            Seat seat = new Seat();

            seat.setSeatNumber("S" + i);
            seat.setBooked(false);
            seat.setShow(show);

            seats.add(seat);
        }

        seatRepository.saveAll(seats);
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

        LocalDateTime updatedDateTime =
                updatedShow.getShowDateTime();

        boolean alreadyExists =
                showRepository
                        .existsByMovieIdAndTheatreIdAndShowTimeAndIdNot(
                                movieId,
                                theatreId,
                                updatedDateTime,
                                showId
                        );

        if (alreadyExists) {
            throw new RuntimeException(
                    "Another show already exists for this movie, theatre and time"
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
    //
    // A show that already has bookings, tickets or payments is never physically
    // removed. It is marked CANCELLED instead so all history stays intact.
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

        if (bookingRepository.existsByShowId(showId)) {

            existingShow.setStatus(ShowStatus.CANCELLED);
            showRepository.save(existingShow);
            return;
        }

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

    private static LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }
}
