package com.srh.cinepass;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.ShowGenerationService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scheduler generation: slot coverage, idempotency, eligibility and concurrent
 * execution. The test properties pin the horizon to one day and the slot list
 * to the four standard showtimes.
 */
class ShowGenerationServiceTests extends TicketTestSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 15);

    @Autowired
    private ShowGenerationService generationService;

    @Test
    void generatesOneShowPerSlotWithExactlyOneSeatPerTheatreSeat() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 8, 0));

        createMovie("Generation Movie");
        Theatre theatre = createAdminTheatre("Generation Theatre", 30);

        ShowGenerationService.GenerationReport report =
                generationService.generateUpcomingShows();

        assertThat(report.executed()).isTrue();
        assertThat(report.created()).isEqualTo(4);
        assertThat(report.failed()).isZero();

        List<Show> shows = showRepository.findByTheatreId(theatre.getId());

        assertThat(shows).hasSize(4);

        assertThat(shows)
                .allSatisfy(show -> {
                    assertThat(show.getShowDate()).isEqualTo(DAY);
                    assertThat(show.getStatus()).isEqualTo(ShowStatus.SCHEDULED);
                    assertThat(seatRepository.countByShowId(show.getId()))
                            .isEqualTo(30);
                });

        assertThat(shows.stream()
                .map(Show::getShowTime)
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder(
                        LocalTime.of(10, 0),
                        LocalTime.of(13, 30),
                        LocalTime.of(17, 0),
                        LocalTime.of(20, 30));
    }

    @Test
    void rerunningGenerationCreatesNoDuplicateShowsOrSeats() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 8, 0));

        createMovie("Idempotent Movie");
        Theatre theatre = createAdminTheatre("Idempotent Theatre", 12);

        generationService.generateUpcomingShows();

        long showsAfterFirstRun = showRepository.count();
        long seatsAfterFirstRun = seatRepository.count();

        ShowGenerationService.GenerationReport second =
                generationService.generateUpcomingShows();

        assertThat(second.executed()).isTrue();
        assertThat(second.created()).isZero();
        assertThat(showRepository.count()).isEqualTo(showsAfterFirstRun);
        assertThat(seatRepository.count()).isEqualTo(seatsAfterFirstRun);
        assertThat(showRepository.findByTheatreId(theatre.getId())).hasSize(4);
    }

    @Test
    void skipsTheatresWhoseOwnerIsNotApproved() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 8, 0));

        createMovie("Eligibility Movie");

        User pendingOwner = createUser("Pending Owner", "THEATRE_OWNER");
        createTheatreVerification(pendingOwner, "PENDING");
        Theatre pendingTheatre = createTheatre(pendingOwner, "Pending Theatre");

        User approvedOwner = createUser("Approved Owner", "THEATRE_OWNER");
        createTheatreVerification(approvedOwner, "APPROVED");
        Theatre approvedTheatre = createTheatre(approvedOwner, "Approved Theatre");

        generationService.generateUpcomingShows();

        assertThat(showRepository.findByTheatreId(pendingTheatre.getId()))
                .isEmpty();
        assertThat(showRepository.findByTheatreId(approvedTheatre.getId()))
                .hasSize(4);
    }

    @Test
    void skipsMoviesWhoseReleaseDateIsInTheFuture() {

        testClock.set(LocalDateTime.of(2026, 11, 15, 8, 0));

        Movie futureMovie = createMovie("Future Movie");
        futureMovie.setReleaseDate(DAY.plusDays(20));
        movieRepository.saveAndFlush(futureMovie);

        createAdminTheatre("Future Theatre", 10);

        ShowGenerationService.GenerationReport report =
                generationService.generateUpcomingShows();

        assertThat(report.created()).isZero();
        assertThat(showRepository.count()).isZero();
    }

    @Test
    void concurrentGenerationRunsStillProduceExactlyOneShowPerSlot() throws Exception {

        testClock.set(LocalDateTime.of(2026, 11, 15, 8, 0));

        createMovie("Concurrent Movie");
        Theatre theatre = createAdminTheatre("Concurrent Theatre", 8);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<ShowGenerationService.GenerationReport> task = () -> {
                start.await(10, TimeUnit.SECONDS);
                return generationService.generateUpcomingShows();
            };

            Future<ShowGenerationService.GenerationReport> first =
                    executor.submit(task);
            Future<ShowGenerationService.GenerationReport> second =
                    executor.submit(task);

            start.countDown();

            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);

        } finally {
            executor.shutdownNow();
        }

        List<Show> shows = showRepository.findByTheatreId(theatre.getId());

        assertThat(shows).hasSize(4);
        assertThat(shows.stream()
                .map(Show::getShowDateTime)
                .distinct()
                .count())
                .isEqualTo(4);

        for (Show show : shows) {
            assertThat(seatRepository.countByShowId(show.getId())).isEqualTo(8);
        }
    }

    private Theatre createAdminTheatre(String name, int totalSeats) {

        Location location = createLocation("Admin City");

        Theatre theatre = new Theatre(name, totalSeats, location);

        return theatreRepository.saveAndFlush(theatre);
    }
}
