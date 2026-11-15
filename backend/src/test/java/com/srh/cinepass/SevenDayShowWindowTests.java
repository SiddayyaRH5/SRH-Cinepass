package com.srh.cinepass;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.service.ShowGenerationLeaseService;
import com.srh.cinepass.service.ShowGenerationService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rolling show window.
 *
 * <p>The horizon is a count of calendar dates, so the default of seven covers
 * today and the next six dates. The window rolls forward on every daily run:
 * the dates that already have shows are skipped and only the missing
 * combinations are created.</p>
 */
@SpringBootTest(properties = {
        "app.shows.scheduler.horizon-days=7"
})
class SevenDayShowWindowTests extends TicketTestSupport {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 11, 16);

    @Autowired
    private ShowGenerationService generationService;

    @Autowired
    private ShowGenerationLeaseService leaseService;

    private Theatre theatre;

    @BeforeEach
    void setUp() {
        testClock.set(LocalDateTime.of(2026, 11, 16, 2, 0));
        theatre = createAdminTheatre("Window Theatre", 40);
    }

    @Test
    void generatesExactlySevenCalendarDates() {

        createMovie("Window Movie");

        generationService.generateUpcomingShows();

        List<LocalDate> dates = generatedDates();

        assertThat(dates).containsExactly(
                DAY_ONE,
                DAY_ONE.plusDays(1),
                DAY_ONE.plusDays(2),
                DAY_ONE.plusDays(3),
                DAY_ONE.plusDays(4),
                DAY_ONE.plusDays(5),
                DAY_ONE.plusDays(6));

        // Never an eighth date.
        assertThat(dates).doesNotContain(DAY_ONE.plusDays(7));
        assertThat(showRepository.count()).isEqualTo(28);
    }

    @Test
    void rollsTheWindowForwardOnTheNextDailyRun() {

        createMovie("Window Movie");

        ShowGenerationService.GenerationReport first =
                generationService.generateUpcomingShows();

        assertThat(first.executed()).isTrue();
        assertThat(first.reason()).isEqualTo("completed");

        long afterFirstRun = showRepository.count();

        assertThat(afterFirstRun).isEqualTo(28);

        // The next day at 2 AM IST.
        testClock.set(LocalDateTime.of(2026, 11, 17, 2, 0));

        ShowGenerationService.GenerationReport report =
                generationService.generateUpcomingShows();

        // The six overlapping dates are skipped, only the new date is created.
        assertThat(report.created()).isEqualTo(4);
        assertThat(showRepository.count()).isEqualTo(32);

        assertThat(generatedDates()).contains(DAY_ONE.plusDays(7));
    }

    @Test
    void rollsTheWindowForwardAgainOnTheThirdDailyRun() {

        createMovie("Window Movie");

        generationService.generateUpcomingShows();

        testClock.set(LocalDateTime.of(2026, 11, 17, 2, 0));
        generationService.generateUpcomingShows();

        testClock.set(LocalDateTime.of(2026, 11, 18, 2, 0));
        generationService.generateUpcomingShows();

        assertThat(showRepository.count()).isEqualTo(36);

        List<LocalDate> dates = generatedDates();

        assertThat(dates).hasSize(9);
        assertThat(dates).contains(DAY_ONE.plusDays(8));
        assertThat(dates).doesNotContain(DAY_ONE.plusDays(9));
    }

    @Test
    void repeatedExecutionNeverCreatesADuplicateShowOrSeat() {

        createMovie("Window Movie");

        generationService.generateUpcomingShows();

        long shows = showRepository.count();
        long seats = seatRepository.count();

        ShowGenerationService.GenerationReport second =
                generationService.generateUpcomingShows();

        assertThat(second.created()).isZero();
        assertThat(showRepository.count()).isEqualTo(shows);
        assertThat(seatRepository.count()).isEqualTo(seats);
    }

    @Test
    void preservesTheFourSlotsAndTheirPricing() {

        createMovie("Window Movie");

        generationService.generateUpcomingShows();

        List<Show> shows = showRepository.findByTheatreId(theatre.getId());

        assertThat(shows).hasSize(28);

        for (Show show : shows) {
            assertThat(show.getStatus().name()).isEqualTo("SCHEDULED");
            assertThat(seatRepository.countByShowId(show.getId())).isEqualTo(40);
        }

        List<Show> today = shows.stream()
                .filter(show -> show.getShowDate().equals(DAY_ONE))
                .collect(Collectors.toList());

        assertThat(today).hasSize(4);

        assertThat(today.stream().map(Show::getShowTime).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(
                        java.time.LocalTime.of(10, 0),
                        java.time.LocalTime.of(13, 30),
                        java.time.LocalTime.of(17, 0),
                        java.time.LocalTime.of(20, 30));

        assertThat(today.stream().map(Show::getTicketPrice).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(180.0, 210.0, 240.0, 270.0);
    }

    @Test
    void neverSchedulesAMovieBeforeItsIndianTheatricalReleaseDate() {

        createMovie("Released Movie");

        Movie futureMovie = createTmdbMovie(
                "Future Movie", 7001L, DAY_ONE.plusDays(3));

        generationService.generateUpcomingShows();

        List<Show> shows = showRepository.findByTheatreId(theatre.getId());

        assertThat(shows).isNotEmpty();

        assertThat(shows)
                .allSatisfy(show -> assertThat(show.getMovie().getId())
                        .isNotEqualTo(futureMovie.getId()));

        // No show of the future movie on a date before its release date.
        assertThat(showRepository.findByMovieId(futureMovie.getId())).isEmpty();
    }

    @Test
    void respectsTheMinimumLeadTime() {

        createMovie("Window Movie");

        // 09:45 IST: the 10:00 slot is only 15 minutes away, and the
        // configured lead time is 30 minutes.
        testClock.set(LocalDateTime.of(2026, 11, 16, 9, 45));

        generationService.generateUpcomingShows();

        List<Show> shows = showRepository.findByTheatreId(theatre.getId());

        // The 10:00 slot of the first date is never created.
        assertThat(shows.stream()
                .filter(show -> show.getShowDate().equals(DAY_ONE))
                .filter(show -> show.getShowTime().equals(java.time.LocalTime.of(10, 0)))
                .count()).isZero();

        // Three usable slots on the first date, four on every later date.
        assertThat(shows).hasSize(3 + 6 * 4);
    }

    @Test
    void neverOverlapsAnExistingShowAtTheTheatre() {

        createMovie("Window Movie");

        Movie other = createMovie("Other Movie");

        Show existing = createShow(other, LocalDateTime.of(2026, 11, 16, 10, 0));

        generationService.generateUpcomingShows();

        List<Show> shows = showRepository.findByTheatreId(theatre.getId());

        assertThat(shows.stream()
                .filter(show -> show.getShowDate().equals(DAY_ONE))
                .filter(show -> show.getShowTime().equals(java.time.LocalTime.of(10, 0)))
                .count()).isEqualTo(1);

        assertThat(showRepository.findById(existing.getId())).isPresent();
    }

    @Test
    void theLeasePreventsASecondInstanceFromGeneratingShows() {

        createMovie("Window Movie");

        leaseService.ensureLeaseRow();

        assertThat(leaseService.tryAcquire("another-instance")).isTrue();

        ShowGenerationService.GenerationReport report =
                generationService.generateUpcomingShows();

        assertThat(report.executed()).isFalse();
        assertThat(report.reason()).isEqualTo("lease-not-acquired");
        assertThat(showRepository.count()).isZero();

        leaseService.release("another-instance");
    }

    @Test
    void doesNotGenerateShowsForAnArchivedMovie() {

        Movie archived = createMovie("Archived Movie");

        archived.setStatus(com.srh.cinepass.entity.MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(archived);

        createMovie("Active Movie");

        generationService.generateUpcomingShows();

        assertThat(showRepository.findByMovieId(archived.getId())).isEmpty();
        assertThat(showRepository.count()).isEqualTo(28);
    }

    @Test
    void doesNotGenerateShowsForAnExcludedMovie() {

        Movie excluded = createMovie("Excluded Movie");

        excluded.setExcludeFromAutoShows(true);
        movieRepository.saveAndFlush(excluded);

        createMovie("Active Movie");

        generationService.generateUpcomingShows();

        assertThat(showRepository.findByMovieId(excluded.getId())).isEmpty();
        assertThat(showRepository.count()).isEqualTo(28);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private List<LocalDate> generatedDates() {
        return showRepository.findByTheatreId(theatre.getId()).stream()
                .map(Show::getShowDate)
                .distinct()
                .sorted()
                .toList();
    }
}
