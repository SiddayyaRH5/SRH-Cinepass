package com.srh.cinepass;

import com.srh.cinepass.dto.BookingRequest;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The customer facing behaviour of a retired movie.
 *
 * <p>An archived movie disappears from the catalogue and can never receive a
 * new booking, while its record, its shows and its whole history stay
 * intact.</p>
 */
class MovieLifecycleApiTests extends TicketTestSupport {

    @Autowired
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 17, 0));
    }

    @Test
    void anArchivedMovieIsNotListedInTheCustomerCatalogue() throws Exception {

        createMovie("Active Movie");
        Movie archived = createMovie("Archived Movie");

        archived.setStatus(MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(archived);

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Active Movie"));
    }

    @Test
    void anArchivedMovieIsNotAccessibleThroughDetailLink() throws Exception {

        Movie archived = createMovie("Archived Movie");

        archived.setStatus(MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(archived);

        mockMvc.perform(get("/api/movies/" + archived.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void thePublicCatalogueNeverLeaksTheAdministratorFields() throws Exception {

        Movie movie = createTmdbMovie("Public Movie", 8001L,
                LocalDateTime.of(2026, 11, 1, 0, 0).toLocalDate());

        movie.setAdminProtected(true);
        movie.setExcludeFromAutoShows(true);
        movie.setExclusionReason("internal note");
        movie.setRetirementHoldReason("internal note");
        movie.setLastSyncError("internal note");
        movie.setActiveUntil(LocalDateTime.of(2026, 12, 1, 0, 0).toLocalDate());
        movie.setTmdbSyncedAt(LocalDateTime.of(2026, 11, 15, 2, 0));
        movieRepository.saveAndFlush(movie);

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Public Movie"))
                .andExpect(jsonPath("$[0].source").doesNotExist())
                .andExpect(jsonPath("$[0].adminProtected").doesNotExist())
                .andExpect(jsonPath("$[0].excludeFromAutoShows").doesNotExist())
                .andExpect(jsonPath("$[0].exclusionReason").doesNotExist())
                .andExpect(jsonPath("$[0].retirementHoldReason").doesNotExist())
                .andExpect(jsonPath("$[0].lastSyncError").doesNotExist())
                .andExpect(jsonPath("$[0].activeUntil").doesNotExist())
                .andExpect(jsonPath("$[0].tmdbSyncedAt").doesNotExist())
                .andExpect(jsonPath("$[0].indiaTheatricalReleaseDate").doesNotExist());
    }

    @Test
    void anArchivedMovieHasNoBookableShowsInTheDiscovery() throws Exception {

        Movie archived = createMovie("Archived Movie");

        createShow(archived, LocalDateTime.of(2026, 11, 16, 20, 0));

        archived.setStatus(MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(archived);

        mockMvc.perform(get("/api/shows/discover/movies/" + archived.getId() + "/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anArchivedMovieCannotReceiveANewBooking() {

        User customer = createUser("Archived Customer", "USER");
        Movie archived = createMovie("Archived Movie");
        Show show = createShow(archived, LocalDateTime.of(2026, 11, 15, 20, 0));
        Seat seat = createSeat(show, "S1");

        archived.setStatus(MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(archived);

        assertThatThrownBy(() -> bookingService.createPendingBooking(
                requestFor(show, seat, customer)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer available");

        assertThat(bookingRepository.count()).isZero();
    }

    @Test
    void anArchivedMovieKeepsItsShowsBookingsAndTickets() throws Exception {

        User customer = createUser("History Customer", "USER");
        Movie archived = createMovie("Archived Movie");
        Show show = createShow(archived, LocalDateTime.of(2026, 11, 15, 20, 0));
        Seat seat = createSeat(show, "S1");

        createConfirmedBooking(customer, show);

        archived.setStatus(MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(archived);

        // The record and its history are all still there.
        assertThat(movieRepository.findById(archived.getId())).isPresent();
        assertThat(showRepository.findById(show.getId())).isPresent();
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(seatRepository.findById(seat.getId())).isPresent();
    }

    @Test
    void anAdministratorCanReactivateAnArchivedMovie() throws Exception {

        User admin = createUser("Reactor Admin", "ADMIN");

        Movie archived = createMovie("Archived Movie");

        archived.setStatus(MovieStatus.ARCHIVED);
        archived.setArchivedAt(LocalDateTime.of(2026, 11, 15, 2, 0));
        movieRepository.saveAndFlush(archived);

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/admin/movies/" + archived.getId() + "/reactivate")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Archived Movie"));
    }

    @Test
    void aMovieWithHistoryCannotBePhysicallyDeleted() throws Exception {

        User admin = createUser("Deleting Admin", "ADMIN");

        Movie movie = createMovie("Historic Movie");
        createShow(movie, LocalDateTime.of(2026, 11, 16, 20, 0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/movies/" + movie.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());

        assertThat(movieRepository.findById(movie.getId())).isPresent();
    }

    @Test
    void aMovieWithoutHistoryCanStillBeDeleted() throws Exception {

        User admin = createUser("Deleting Admin", "ADMIN");

        Movie movie = createMovie("Throwaway Movie");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/movies/" + movie.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(movieRepository.findById(movie.getId())).isEmpty();
    }

    @Test
    void nonexistentMovie404IsNotCached() throws Exception {
        Long nonexistentId = 999999L;

        // First call: 404
        mockMvc.perform(get("/api/movies/" + nonexistentId))
                .andExpect(status().isNotFound());

        // Create the movie
        Movie movie = createMovie("Previously Missing Movie");
        movie.setTmdbId(nonexistentId);
        movieRepository.saveAndFlush(movie);

        // Second call: should find the movie (404 was not cached)
        mockMvc.perform(get("/api/movies/" + movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movie.getId()));
    }

    @Test
    void reactivatedMovieIsAccessibleThroughDetailLink() throws Exception {
        User admin = createUser("Reactor Admin", "ADMIN");

        Movie archived = createMovie("Archived Movie");
        archived.setStatus(MovieStatus.ARCHIVED);
        archived.setArchivedAt(LocalDateTime.of(2026, 11, 15, 2, 0));
        movieRepository.saveAndFlush(archived);

        // Archived movie is not accessible
        mockMvc.perform(get("/api/movies/" + archived.getId()))
                .andExpect(status().isNotFound());

        // Reactivate the movie
        mockMvc.perform(post("/api/admin/movies/" + archived.getId() + "/reactivate")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        // Reactivated movie is now accessible
        mockMvc.perform(get("/api/movies/" + archived.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(archived.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private BookingRequest requestFor(Show show, Seat seat, User user) {
        BookingRequest request = new BookingRequest();
        request.setShowId(show.getId());
        request.setSeatIds(List.of(seat.getId()));
        request.setUserId(String.valueOf(user.getId()));
        request.setUserEmail(user.getEmail());
        return request;
    }

    private Seat createSeat(Show show, String seatNumber) {
        Seat seat = new Seat();
        seat.setSeatNumber(seatNumber);
        seat.setShow(show);
        return seatRepository.saveAndFlush(seat);
    }
}
