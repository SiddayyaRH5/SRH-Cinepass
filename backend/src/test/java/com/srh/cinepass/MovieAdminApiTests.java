package com.srh.cinepass;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieSource;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The administrator API for the automated movie catalogue.
 *
 * <p>Every endpoint must stay behind the ADMIN role. The tests also cover
 * the catalogue controls themselves and the synchronisation report, including
 * the duplicate and missing TMDB identifiers.</p>
 */
class MovieAdminApiTests extends TicketTestSupport {

    @BeforeEach
    void setUp() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 2, 0));
    }

    @Test
    void theAdminEndpointsRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/admin/movies"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/movies/imported"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/movies/sync-status"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/automation/runs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theAdminEndpointsRequireTheAdminRole() throws Exception {

        User customer = createUser("Regular Customer", "USER");
        User owner = createUser("Theatre Owner", "THEATRE_OWNER");

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/movies/1/reactivate")
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdministratorSeesEveryMovieIncludingTheRetiredOnes() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        createMovie("Active Movie");

        Movie archived = createMovie("Archived Movie");
        archived.setStatus(MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(archived);

        mockMvc.perform(get("/api/admin/movies")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void anAdministratorSeesOnlyTheImportedMoviesInTheImportedList() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        createMovie("Manual Movie");

        Movie imported = createTmdbMovie("Imported Movie", 8101L,
                LocalDate.of(2026, 11, 1));

        mockMvc.perform(get("/api/admin/movies/imported")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(imported.getId()))
                .andExpect(jsonPath("$[0].source").value("TMDB"));
    }

    @Test
    void anAdministratorCanExcludeAMovieFromAutomaticShowGeneration() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        Movie movie = createMovie("Excludable Movie");

        mockMvc.perform(post("/api/admin/movies/" + movie.getId() + "/exclude")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"excluded\":true,\"reason\":\"no screens\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.excludedFromAutoShows").value(true))
                .andExpect(jsonPath("$.exclusionReason").value("no screens"));

        Movie stored = movieRepository.findById(movie.getId()).orElseThrow();

        assertThat(stored.isExcludedFromAutoShows()).isTrue();

        // The movie stays in the customer catalogue.
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void anAdministratorCanProtectAMovieFromRetirement() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        Movie movie = createTmdbMovie("Protectable Movie", 8102L,
                LocalDate.of(2026, 1, 1));

        mockMvc.perform(post("/api/admin/movies/" + movie.getId() + "/protect")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"protectedMovie\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminProtected").value(true));

        assertThat(movieRepository.findById(movie.getId())
                .orElseThrow()
                .isAdminProtected()).isTrue();
    }

    @Test
    void anAdministratorCanExtendTheActivePeriodOfAMovie() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        Movie movie = createTmdbMovie("Extendable Movie", 8103L,
                LocalDate.of(2026, 1, 1));

        mockMvc.perform(post("/api/admin/movies/" + movie.getId() + "/extend")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activeUntil\":\"2026-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUntil").value("2026-12-31"));
    }

    @Test
    void anInvalidActivePeriodIsRejected() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        Movie movie = createTmdbMovie("Extendable Movie", 8104L,
                LocalDate.of(2026, 1, 1));

        mockMvc.perform(post("/api/admin/movies/" + movie.getId() + "/extend")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activeUntil\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownMovieIsRejected() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        mockMvc.perform(post("/api/admin/movies/999999/reactivate")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theSyncStatusReportsDuplicateAndMissingTmdbIdentifiers() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        Movie duplicateOne = new Movie();
        duplicateOne.setTitle("Duplicate One");
        duplicateOne.setTmdbId(8201L);
        duplicateOne.setSource(MovieSource.TMDB);
        movieRepository.saveAndFlush(duplicateOne);

        Movie duplicateTwo = new Movie();
        duplicateTwo.setTitle("Duplicate Two");
        duplicateTwo.setTmdbId(8201L);
        duplicateTwo.setSource(MovieSource.TMDB);
        movieRepository.saveAndFlush(duplicateTwo);

        createMovie("No Tmdb Id Movie");

        mockMvc.perform(get("/api/admin/movies/sync-status")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovies").value(3))
                .andExpect(jsonPath("$.tmdbManagedMovies").value(2))
                .andExpect(jsonPath("$.manualMovies").value(1))
                .andExpect(jsonPath("$.moviesWithoutTmdbId").value(1))
                .andExpect(jsonPath("$.duplicateTmdbIds[0]").value(8201));

        // The duplicates are only reported. Nothing is merged or deleted.
        assertThat(movieRepository.count()).isEqualTo(3);
    }

    @Test
    void theAutomationRunHistoryIsAdminOnly() throws Exception {

        User admin = createUser("Catalogue Admin", "ADMIN");

        mockMvc.perform(get("/api/admin/automation/runs")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
