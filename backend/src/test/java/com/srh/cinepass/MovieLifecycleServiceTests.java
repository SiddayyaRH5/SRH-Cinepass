package com.srh.cinepass;

import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.MovieLifecycleService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automatic movie retirement.
 *
 * <p>The retirement age is measured from the confirmed Indian theatrical
 * release date. A movie is retired once the age reaches the configured number
 * of days (60 by default), and it is never retired while anything about it is
 * still open. Nothing is ever deleted.</p>
 */
class MovieLifecycleServiceTests extends TicketTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    @Autowired
    private MovieLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 2, 0));
    }

    @Test
    void keepsAMovieThatIsYoungerThanTheRetirementAge() {

        Movie movie = createTmdbMovie("Young Movie", 5001L, TODAY.minusDays(59));

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isZero();
        assertThat(stored(movie).getStatus()).isEqualTo(MovieStatus.ACTIVE);
    }

    @Test
    void retiresAMovieExactlyAtTheRetirementBoundary() {

        Movie movie = createTmdbMovie("Boundary Movie", 5002L, TODAY.minusDays(60));

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isEqualTo(1);

        Movie retired = stored(movie);

        assertThat(retired.getStatus()).isEqualTo(MovieStatus.ARCHIVED);
        assertThat(retired.getArchivedAt()).isNotNull();
    }

    @Test
    void retiresAMovieThatIsOlderThanTheRetirementAge() {

        Movie movie = createTmdbMovie("Old Movie", 5003L, TODAY.minusDays(90));

        lifecycleService.evaluate();

        assertThat(stored(movie).getStatus()).isEqualTo(MovieStatus.ARCHIVED);
    }

    @Test
    void neverDeletesTheRetiredMovieOrItsShows() {

        Movie movie = createTmdbMovie("Historic Movie", 5004L, TODAY.minusDays(120));

        Show pastShow = createShow(movie, LocalDateTime.of(2026, 11, 10, 10, 0));

        lifecycleService.evaluate();

        assertThat(movieRepository.findById(movie.getId())).isPresent();
        assertThat(showRepository.findById(pastShow.getId())).isPresent();
        assertThat(showRepository.findByMovieId(movie.getId())).hasSize(1);
    }

    @Test
    void doesNotRetireAMovieThatStillHasFutureShows() {

        Movie movie = createTmdbMovie("Future Show Movie", 5005L, TODAY.minusDays(90));

        createShow(movie, LocalDateTime.of(2026, 11, 16, 10, 0));

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isZero();
        assertThat(report.protectedCount()).isEqualTo(1);

        Movie stored = stored(movie);

        assertThat(stored.getStatus()).isEqualTo(MovieStatus.ACTIVE);
        assertThat(stored.getRetirementHoldReason())
                .contains("future show");
    }

    @Test
    void doesNotRetireAMovieWithAnOpenBooking() {

        Movie movie = createTmdbMovie("Booked Movie", 5006L, TODAY.minusDays(90));

        User customer = createUser("Customer", "USER");

        Show pastShow = createShow(movie, LocalDateTime.of(2026, 11, 10, 10, 0));

        createConfirmedBooking(customer, pastShow);

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isZero();

        Movie stored = stored(movie);

        assertThat(stored.getStatus()).isEqualTo(MovieStatus.ACTIVE);
        assertThat(stored.getRetirementHoldReason())
                .contains("booking");
    }

    @Test
    void doesNotRetireAMovieWithAnUnresolvedPaymentWorkflow() {

        Movie movie = createTmdbMovie("Unresolved Movie", 5007L, TODAY.minusDays(90));

        User customer = createUser("Customer", "USER");

        Show pastShow = createShow(movie, LocalDateTime.of(2026, 11, 10, 10, 0));

        Booking booking = createBooking(customer, pastShow, "CANCELLED", "PAID");

        booking.setPaymentRecoveryStatus("MANUAL_REVIEW");
        booking.setPaymentRecoveryReason("Captured payment needs a manual review");
        bookingRepository.saveAndFlush(booking);

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isZero();

        Movie stored = stored(movie);

        assertThat(stored.getStatus()).isEqualTo(MovieStatus.ACTIVE);
        assertThat(stored.getRetirementHoldReason())
                .contains("payment workflow");
    }

    @Test
    void doesNotRetireAnAdministratorProtectedMovie() {

        Movie movie = createTmdbMovie("Protected Movie", 5008L, TODAY.minusDays(90));

        movie.setAdminProtected(true);
        movieRepository.saveAndFlush(movie);

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isZero();
        assertThat(stored(movie).getRetirementHoldReason())
                .contains("protected by an administrator");
    }

    @Test
    void doesNotRetireAMovieWhoseActivePeriodWasExtended() {

        Movie movie = createTmdbMovie("Extended Movie", 5009L, TODAY.minusDays(90));

        movie.setActiveUntil(TODAY.plusDays(30));
        movieRepository.saveAndFlush(movie);

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isZero();
        assertThat(stored(movie).getRetirementHoldReason())
                .contains("active period extended");
    }

    @Test
    void retiresAMovieOnceItsExtendedActivePeriodHasPassed() {

        Movie movie = createTmdbMovie("Expired Extension", 5010L, TODAY.minusDays(90));

        movie.setActiveUntil(TODAY.minusDays(1));
        movieRepository.saveAndFlush(movie);

        lifecycleService.evaluate();

        assertThat(stored(movie).getStatus()).isEqualTo(MovieStatus.ARCHIVED);
    }

    @Test
    void fallsBackToTheLegacyReleaseDateWhenNoIndianDateIsKnown() {

        Movie movie = createMovie("Legacy Movie");

        movie.setReleaseDate(TODAY.minusDays(90));
        movieRepository.saveAndFlush(movie);

        lifecycleService.evaluate();

        assertThat(stored(movie).getStatus()).isEqualTo(MovieStatus.ARCHIVED);
    }

    @Test
    void neverRetiresAMovieWithoutAnyReleaseDate() {

        Movie movie = createMovie("Undated Movie");

        lifecycleService.evaluate();

        // A legacy row carries no status at all, which means ACTIVE.
        assertThat(stored(movie).isActive()).isTrue();
        assertThat(stored(movie).isArchived()).isFalse();
    }

    @Test
    void doesNotRetireAMovieThatIsAlreadyArchived() {

        Movie movie = createTmdbMovie("Archived Movie", 5011L, TODAY.minusDays(200));

        movie.setStatus(MovieStatus.ARCHIVED);
        movieRepository.saveAndFlush(movie);

        MovieLifecycleService.LifecycleReport report = lifecycleService.evaluate();

        assertThat(report.archived()).isZero();
        assertThat(report.evaluated()).isZero();
    }

    @Test
    void isSafeToRunTwice() {

        Movie movie = createTmdbMovie("Repeated Movie", 5012L, TODAY.minusDays(90));

        lifecycleService.evaluate();
        lifecycleService.evaluate();

        assertThat(stored(movie).getStatus()).isEqualTo(MovieStatus.ARCHIVED);
        assertThat(stored(movie).getRetirementHoldReason()).isNull();
    }

    private Movie stored(Movie movie) {
        return movieRepository.findById(movie.getId()).orElseThrow();
    }
}
