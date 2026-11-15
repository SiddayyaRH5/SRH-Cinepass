package com.srh.cinepass;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Movie-based discovery APIs: only bookable cities, theatres, dates and
 * showtimes are returned. Filtering happens in the repository, so expired,
 * cancelled, already-started and unapproved-owner data never reaches the UI.
 */
class ShowDiscoveryTests extends TicketTestSupport {

    @Test
    void discoveryReturnsOnlyBookableCitiesTheatresDatesAndTimes() throws Exception {

        testClock.set(LocalDateTime.of(2026, 11, 15, 8, 0));

        Movie movie = createMovie("Discovery Movie");

        Location cityA = createLocation("Discovery City A");
        Theatre adminTheatre = new Theatre("Admin Discovery Theatre", 20, cityA);
        theatreRepository.saveAndFlush(adminTheatre);

        Location cityB = createLocation("Discovery City B");
        User pendingOwner = createUser("Discovery Pending Owner", "THEATRE_OWNER");
        createTheatreVerification(pendingOwner, "PENDING");

        Theatre pendingTheatre = new Theatre("Pending Discovery Theatre", 20, cityB);
        pendingTheatre.setOwner(pendingOwner);
        theatreRepository.saveAndFlush(pendingTheatre);

        Show bookable = createShowAt(
                adminTheatre,
                movie,
                LocalDateTime.of(2026, 11, 15, 18, 0),
                ShowStatus.SCHEDULED);
        createSeats(bookable, 5);

        // Already started -> excluded by the booking window.
        createShowAt(
                adminTheatre,
                movie,
                LocalDateTime.of(2026, 11, 15, 7, 0),
                ShowStatus.SCHEDULED);

        Show cancelled = createShowAt(
                adminTheatre,
                movie,
                LocalDateTime.of(2026, 11, 15, 19, 0),
                ShowStatus.CANCELLED);

        // Future date but expired status -> excluded everywhere.
        createShowAt(
                adminTheatre,
                movie,
                LocalDateTime.of(2026, 11, 16, 18, 0),
                ShowStatus.EXPIRED);

        // Unapproved owner -> theatre and city excluded.
        createShowAt(
                pendingTheatre,
                movie,
                LocalDateTime.of(2026, 11, 15, 18, 0),
                ShowStatus.SCHEDULED);

        mockMvc.perform(get("/api/shows/discover/movies/{movieId}/cities", movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(cityA.getId().intValue()))
                .andExpect(jsonPath("$[0].city").value(cityA.getCity()));

        mockMvc.perform(get(
                        "/api/shows/discover/movies/{movieId}/cities/{locationId}/theatres",
                        movie.getId(),
                        cityA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(adminTheatre.getId().intValue()))
                .andExpect(jsonPath("$[0].name").value("Admin Discovery Theatre"));

        mockMvc.perform(get(
                        "/api/shows/discover/movies/{movieId}/theatres/{theatreId}/dates",
                        movie.getId(),
                        adminTheatre.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].date").value("2026-11-15"));

        mockMvc.perform(get(
                        "/api/shows/discover/movies/{movieId}/theatres/{theatreId}/dates/{date}",
                        movie.getId(),
                        adminTheatre.getId(),
                        "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(bookable.getId().intValue()))
                .andExpect(jsonPath("$[0].showDate").value("2026-11-15"))
                .andExpect(jsonPath("$[0].showTime").value("18:00:00"))
                .andExpect(jsonPath("$[0].totalSeats").value(5))
                .andExpect(jsonPath("$[0].availableSeats").value(5));

        mockMvc.perform(get("/api/shows/{showId}/availability", bookable.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookable").value(true))
                .andExpect(jsonPath("$.totalSeats").value(5))
                .andExpect(jsonPath("$.availableSeats").value(5))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(get("/api/shows/{showId}/availability", cancelled.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookable").value(false))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void availabilityCountsBookedAndReservedSeats() throws Exception {

        testClock.set(LocalDateTime.of(2026, 11, 15, 8, 0));

        Movie movie = createMovie("Availability Movie");
        Location location = createLocation("Availability City");
        Theatre theatre = new Theatre("Availability Theatre", 10, location);
        theatreRepository.saveAndFlush(theatre);

        Show show = createShowAt(
                theatre,
                movie,
                LocalDateTime.of(2026, 11, 15, 18, 0),
                ShowStatus.SCHEDULED);

        List<Seat> seats = createSeats(show, 4);

        Seat permanentlyBooked = seats.get(0);
        permanentlyBooked.setBooked(true);
        seatRepository.saveAndFlush(permanentlyBooked);

        Seat temporarilyReserved = seats.get(1);
        temporarilyReserved.setReservedByBookingId(999L);
        temporarilyReserved.setReservedUntil(LocalDateTime.of(2026, 11, 15, 8, 30));
        seatRepository.saveAndFlush(temporarilyReserved);

        mockMvc.perform(get("/api/shows/{showId}/availability", show.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSeats").value(4))
                .andExpect(jsonPath("$.availableSeats").value(2));
    }

    private Show createShowAt(
            Theatre theatre,
            Movie movie,
            LocalDateTime showTime,
            ShowStatus status) {

        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowTime(showTime);
        show.setTicketPrice(200.0);
        show.setStatus(status);

        return showRepository.saveAndFlush(show);
    }

    private List<Seat> createSeats(Show show, int count) {

        List<Seat> seats = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {
            Seat seat = new Seat();
            seat.setSeatNumber("S" + i);
            seat.setShow(show);
            seats.add(seat);
        }

        return seatRepository.saveAllAndFlush(seats);
    }
}
