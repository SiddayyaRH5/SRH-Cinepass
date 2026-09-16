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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;

    public ShowService(
            ShowRepository showRepository,
            MovieRepository movieRepository,
            TheatreRepository theatreRepository,
            SeatRepository seatRepository) {

        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.seatRepository = seatRepository;
    }

    public List<Show> getAllShows() {
        return showRepository.findAll();
    }

    public Optional<Show> getShowById(Long id) {
        return showRepository.findById(id);
    }

    public Show addShow(Show show) {

        // Get the real movie from database
        Movie movie = movieRepository.findById(show.getMovie().getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Movie not found with id: "
                                        + show.getMovie().getId()));

        // Get the real theatre from database
        Theatre theatre = theatreRepository.findById(show.getTheatre().getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Theatre not found with id: "
                                        + show.getTheatre().getId()));

        // Replace incomplete objects with real database objects
        show.setMovie(movie);
        show.setTheatre(theatre);

        // Save show
        Show savedShow = showRepository.save(show);

        // Get actual number of seats from theatre
        Integer totalSeats = theatre.getTotalSeats();

        // Create seats automatically for this show
        for (int i = 1; i <= totalSeats; i++) {

            Seat seat = new Seat();

            seat.setSeatNumber("S" + i);
            seat.setBooked(false);
            seat.setShow(savedShow);

            seatRepository.save(seat);
        }

        return savedShow;
    }

    public Show updateShow(Long id, Show showDetails) {

        Show show = showRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Show not found with id: " + id));

        Movie movie = movieRepository.findById(
                        showDetails.getMovie().getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Movie not found with id: "
                                        + showDetails.getMovie().getId()));

        Theatre theatre = theatreRepository.findById(
                        showDetails.getTheatre().getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Theatre not found with id: "
                                        + showDetails.getTheatre().getId()));

        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowDate(showDetails.getShowDate());
        show.setShowTime(showDetails.getShowTime());
        show.setTicketPrice(showDetails.getTicketPrice());

        return showRepository.save(show);
    }

    public void deleteShow(Long id) {

        if (!showRepository.existsById(id)) {
            throw new RuntimeException(
                    "Show not found with id: " + id);
        }

        showRepository.deleteById(id);
    }

    public List<Show> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId);
    }

    public List<Show> getShowsByTheatre(Long theatreId) {
        return showRepository.findByTheatreId(theatreId);
    }

    public List<Show> getShowsByDate(LocalDate date) {
        return showRepository.findByShowDate(date);
    }

    public List<Show> getShowsByMovieAndDate(
            Long movieId,
            LocalDate date) {

        return showRepository.findByMovieIdAndShowDate(
                movieId,
                date);
    }
    // ==============================
    // GET SEATS FOR SHOW
    // ==============================

    public List<Seat> getSeatsByShow(Long showId) {

        return seatRepository.findByShowId(showId);
    }
    // ==========================================
// GET SHOWS BY THEATRE AND DATE
// ==========================================

public List<Show> getShowsByTheatreAndDate(
        Long theatreId,
        LocalDate date) {

    return showRepository.findByTheatreIdAndShowDate(
            theatreId,
            date
    );
}
}