package com.srh.cinepass.service;

import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.repository.BookingRepository;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.repository.ShowRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Automatic movie retirement.
 *
 * <p>A movie becomes eligible for retirement once it is
 * {@code app.movies.lifecycle.retirement-days} (default 14) calendar days past
 * its Indian theatrical release date. The reference date is the confirmed
 * Indian theatrical date; the legacy {@code releaseDate} is only used as a
 * fallback for rows that predate the India date field.</p>
 *
 * <p><strong>Boundary rule:</strong> a movie is eligible on the day the age
 * reaches exactly the configured number of days. A movie released 14 days ago
 * is eligible; a movie released 13 days ago is not.</p>
 *
 * <p>Retirement never deletes anything. It moves the movie to
 * {@code ARCHIVED}, which hides it from the customer catalogue, stops new
 * shows and blocks new bookings. Existing shows, bookings, tickets, seats and
 * payment records are preserved exactly as they are.</p>
 *
 * <p>A movie is <strong>not</strong> retired while any of the following is
 * true. In that case the retirement is postponed and the reason is recorded
 * on the movie so an administrator can see it:</p>
 * <ul>
 *   <li>it still has future scheduled shows that must be honoured,</li>
 *   <li>it has bookings that still have to be fulfilled,</li>
 *   <li>it has an unresolved booking or payment workflow,</li>
 *   <li>an administrator protected it,</li>
 *   <li>an administrator extended its active period.</li>
 * </ul>
 */
@Service
public class MovieLifecycleService {

    private static final Logger log =
            LoggerFactory.getLogger(MovieLifecycleService.class);

    /**
     * Bookings that still have to be fulfilled. A cancelled booking does not
     * block retirement.
     */
    private static final List<String> OPEN_BOOKING_STATUSES = List.of(
            "PENDING",
            "CONFIRMED",
            BookingService.STATUS_PAYMENT_REVIEW);

    /**
     * Outcome of one lifecycle pass.
     *
     * @param evaluated movies that were examined
     * @param archived  movies that were retired
     * @param protectedCount movies whose retirement was postponed
     * @param skipped   movies that were not old enough to retire
     * @param reasons   short descriptions of the postponements
     */
    public record LifecycleReport(
            int evaluated,
            int archived,
            int protectedCount,
            int skipped,
            List<String> reasons) {
    }

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final MovieLifecycleWriter writer;
    private final Clock clock;

    private final boolean retirementEnabled;
    private final int retirementDays;

    public MovieLifecycleService(
            MovieRepository movieRepository,
            ShowRepository showRepository,
            BookingRepository bookingRepository,
            MovieLifecycleWriter writer,
            Clock clock,
            @Value("${app.movies.lifecycle.retirement-enabled:true}") boolean retirementEnabled,
            @Value("${app.movies.lifecycle.retirement-days:14}") int retirementDays) {

        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.bookingRepository = bookingRepository;
        this.writer = writer;
        this.clock = clock;
        this.retirementEnabled = retirementEnabled;
        this.retirementDays = Math.max(1, retirementDays);
    }

    /**
     * Runs one lifecycle pass. Never throws for a single movie failure.
     */
    public LifecycleReport evaluate() {

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        if (!retirementEnabled) {
            log.info("Movie retirement is disabled; nothing to evaluate");
            return new LifecycleReport(0, 0, 0, 0, List.of());
        }

        List<Movie> movies = movieRepository.findActiveMoviesForLifecycle(MovieStatus.ACTIVE);

        int archived = 0;
        int protectedCount = 0;
        int skipped = 0;
        List<String> reasons = new ArrayList<>();

        for (Movie movie : movies) {

            try {
                LocalDate referenceDate = referenceDate(movie);

                if (referenceDate == null) {
                    skipped++;
                    continue;
                }

                long ageInDays = ChronoUnit.DAYS.between(referenceDate, today);

                if (ageInDays < retirementDays) {
                    skipped++;
                    continue;
                }

                String hold = findHoldReason(movie, today, now);

                if (hold != null) {
                    protectedCount++;
                    reasons.add(movie.getTitle() + ": " + hold);

                    writer.recordHold(movie.getId(), hold);

                    log.info("Postponed retirement of movie {}: {}", movie.getId(), hold);
                    continue;
                }

                // Archive movies without a valid poster path. A movie without a
                // poster is not a valid catalogue entry. This check runs after
                // the hold check so movies with active dependencies (future
                // shows, open bookings, unresolved payments, etc.) are never
                // archived prematurely.
                String posterUrl = movie.getPosterUrl();
                if (posterUrl == null || posterUrl.isBlank()) {
                    writer.archive(movie.getId(), now);
                    archived++;
                    log.info("Archived movie {} ({}) - missing poster", movie.getId(), movie.getTitle());
                    continue;
                }

                writer.archive(movie.getId(), now);

                archived++;

                log.info(
                        "Retired movie {} ({}), Indian theatrical date {}",
                        movie.getId(),
                        movie.getTitle(),
                        referenceDate);

            } catch (RuntimeException ex) {
                skipped++;
                reasons.add("movie " + movie.getId() + ": " + ex.getMessage());
                log.warn("Failed to evaluate movie {}: {}", movie.getId(), ex.getMessage());
            }
        }

        log.info(
                "Movie lifecycle finished: evaluated={}, archived={}, protected={}, skipped={}",
                movies.size(),
                archived,
                protectedCount,
                skipped);

        return new LifecycleReport(movies.size(), archived, protectedCount, skipped, reasons);
    }

    /**
     * The date the retirement age is measured from.
     *
     * <p>The confirmed Indian theatrical release date is authoritative. The
     * legacy {@code releaseDate} is only a fallback for rows that were
     * created before the India date field existed.</p>
     */
    private LocalDate referenceDate(Movie movie) {

        if (movie.getIndiaTheatricalReleaseDate() != null) {
            return movie.getIndiaTheatricalReleaseDate();
        }

        return movie.getReleaseDate();
    }

    /**
     * @return the reason why the retirement must be postponed, or {@code null}
     *         when the movie can be retired
     */
    private String findHoldReason(Movie movie, LocalDate today, LocalDateTime now) {

        if (movie.isAdminProtected()) {
            return "protected by an administrator";
        }

        LocalDate activeUntil = movie.getActiveUntil();

        if (activeUntil != null && !today.isAfter(activeUntil)) {
            return "active period extended until " + activeUntil;
        }

        long futureShows = showRepository.countFutureScheduledShows(
                movie.getId(),
                now,
                ShowStatus.SCHEDULED);

        if (futureShows > 0) {
            return futureShows + " future show(s) still scheduled";
        }

        long openBookings = bookingRepository.countActiveBookingsForMovie(
                movie.getId(),
                OPEN_BOOKING_STATUSES);

        if (openBookings > 0) {
            return openBookings + " booking(s) still open";
        }

        long unresolvedPayments = bookingRepository
                .countUnresolvedPaymentWorkflowsForMovie(
                        movie.getId(),
                        BookingService.RECOVERY_MANUAL_REVIEW);

        if (unresolvedPayments > 0) {
            return unresolvedPayments + " unresolved payment workflow(s)";
        }

        return null;
    }
}
