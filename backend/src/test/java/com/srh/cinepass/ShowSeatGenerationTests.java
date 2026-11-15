package com.srh.cinepass;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.service.ShowService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Seat generation for manually created and scheduler-created shows. Seats are
 * created exactly once, match the theatre seat count, and an existing booked
 * seat is never regenerated.
 */
class ShowSeatGenerationTests extends TicketTestSupport {

    @Autowired
    private ShowService showService;

    @Test
    void addShowCreatesExactlyTheTheatreSeatCountAndNeverDuplicatesSeats() {

        Movie movie = createMovie("Seat Movie");

        Location location = createLocation("Seat City");
        Theatre theatre = new Theatre("Seat Theatre", 25, location);
        theatreRepository.saveAndFlush(theatre);

        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowDate(LocalDate.of(2026, 11, 20));
        show.setShowTime(LocalTime.of(18, 0));
        show.setTicketPrice(200.0);

        Show saved = showService.addShow(show, null, true);

        assertThat(seatRepository.countByShowId(saved.getId())).isEqualTo(25);

        List<Seat> seats = seatRepository.findByShowId(saved.getId());
        Seat first = seats.get(0);
        first.setBooked(true);
        seatRepository.saveAndFlush(first);

        // Calling seat generation again is a no-op.
        showService.createSeatsForShow(saved, 25);

        assertThat(seatRepository.countByShowId(saved.getId())).isEqualTo(25);
        assertThat(seatRepository.findById(first.getId()).orElseThrow().isPermanentlyBooked())
                .isTrue();
    }

    @Test
    void addShowRejectsADuplicateMovieTheatreAndExactShowtime() {

        Movie movie = createMovie("Duplicate Movie");

        Location location = createLocation("Duplicate City");
        Theatre theatre = new Theatre("Duplicate Theatre", 10, location);
        theatreRepository.saveAndFlush(theatre);

        Show first = new Show();
        first.setMovie(movie);
        first.setTheatre(theatre);
        first.setShowDate(LocalDate.of(2026, 11, 20));
        first.setShowTime(LocalTime.of(18, 0));
        first.setTicketPrice(200.0);

        showService.addShow(first, null, true);

        Show duplicate = new Show();
        duplicate.setMovie(movie);
        duplicate.setTheatre(theatre);
        duplicate.setShowDate(LocalDate.of(2026, 11, 20));
        duplicate.setShowTime(LocalTime.of(18, 0));
        duplicate.setTicketPrice(250.0);

        assertThatThrownBy(() -> showService.addShow(duplicate, null, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        assertThat(showRepository.findByTheatreId(theatre.getId())).hasSize(1);
        assertThat(seatRepository.countByShowId(
                showRepository.findByTheatreId(theatre.getId()).get(0).getId()))
                .isEqualTo(10);
    }

    @Test
    void theSameMovieMayRunAtSeveralShowtimesOnTheSameDay() {

        Movie movie = createMovie("Multi Slot Movie");

        Location location = createLocation("Multi Slot City");
        Theatre theatre = new Theatre("Multi Slot Theatre", 10, location);
        theatreRepository.saveAndFlush(theatre);

        for (LocalTime time : List.of(
                LocalTime.of(10, 0),
                LocalTime.of(18, 0))) {

            Show show = new Show();
            show.setMovie(movie);
            show.setTheatre(theatre);
            show.setShowDate(LocalDate.of(2026, 11, 20));
            show.setShowTime(time);
            show.setTicketPrice(200.0);

            showService.addShow(show, null, true);
        }

        assertThat(showRepository.findByTheatreId(theatre.getId())).hasSize(2);
    }

    @Test
    void deletingAShowWithBookingsCancelsItInsteadOfDeletingHistory() {

        Movie movie = createMovie("History Movie");

        Location location = createLocation("History City");
        Theatre theatre = new Theatre("History Theatre", 10, location);
        theatreRepository.saveAndFlush(theatre);

        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowDate(LocalDate.of(2026, 11, 20));
        show.setShowTime(LocalTime.of(18, 0));
        show.setTicketPrice(200.0);

        Show saved = showService.addShow(show, null, true);

        createBooking(createUser("History Customer", "USER"), saved, "CONFIRMED", "PAID");

        showService.deleteShow(saved.getId(), null, true);

        Show reloaded = showRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(
                com.srh.cinepass.entity.ShowStatus.CANCELLED);
        assertThat(seatRepository.countByShowId(saved.getId())).isEqualTo(10);
    }
}
