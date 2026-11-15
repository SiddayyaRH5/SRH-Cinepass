package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.repository.ShowRepository;
import com.srh.cinepass.repository.TheatreRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Creates the daily show grid.
 *
 * <p>For each day in the configured horizon it walks every eligible theatre and
 * every configured slot, picks an eligible movie deterministically, and asks
 * {@link ShowService} to create the show and its seats. Generation is
 * idempotent: the exact showtime is the uniqueness key, so reruns and retries
 * never create a second show or a second seat row.</p>
 *
 * <p>{@code app.shows.scheduler.horizon-days} is the <em>number of calendar
 * dates</em> in the rolling window, not the number of days in the future. A
 * value of {@code 7} therefore covers today and the next six dates, which is
 * exactly seven dates. The minimum of one keeps the window valid even when
 * the property is configured to zero.</p>
 *
 * <p>Cross-instance safety has two layers: a database lease (best effort) and
 * the database uniqueness constraint (authoritative). A constraint violation
 * is counted as a skip, never as a fatal error.</p>
 */
@Service
public class ShowGenerationService {

    private static final Logger log =
            LoggerFactory.getLogger(ShowGenerationService.class);

    private static final List<LocalTime> DEFAULT_SLOTS = List.of(
            LocalTime.of(10, 0),
            LocalTime.of(13, 30),
            LocalTime.of(17, 0),
            LocalTime.of(20, 30)
    );

    private final ShowService showService;
    private final ShowGenerationLeaseService leaseService;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ShowRepository showRepository;
    private final Clock clock;

    private final int horizonDays;
    private final int minLeadMinutes;
    private final int moviesPerTheatrePerDay;
    private final double baseTicketPrice;
    private final double slotPriceStep;
    private final List<LocalTime> slots;

    public ShowGenerationService(
            ShowService showService,
            ShowGenerationLeaseService leaseService,
            MovieRepository movieRepository,
            TheatreRepository theatreRepository,
            ShowRepository showRepository,
            Clock clock,
            @Value("${app.shows.scheduler.horizon-days:7}") int horizonDays,
            @Value("${app.shows.scheduler.min-lead-minutes:30}") int minLeadMinutes,
            @Value("${app.shows.scheduler.movies-per-theatre-per-day:4}")
            int moviesPerTheatrePerDay,
            @Value("${app.shows.scheduler.base-ticket-price:180}")
            double baseTicketPrice,
            @Value("${app.shows.scheduler.slot-price-step:30}")
            double slotPriceStep,
            @Value("${app.shows.scheduler.slots:10:00,13:30,17:00,20:30}")
            String slots) {

        this.showService = showService;
        this.leaseService = leaseService;
        this.movieRepository = movieRepository;
        this.theatreRepository = theatreRepository;
        this.showRepository = showRepository;
        this.clock = clock;

        this.horizonDays = Math.max(1, horizonDays);
        this.minLeadMinutes = Math.max(0, minLeadMinutes);
        this.moviesPerTheatrePerDay = Math.max(1, moviesPerTheatrePerDay);
        this.baseTicketPrice = Math.max(0, baseTicketPrice);
        this.slotPriceStep = Math.max(0, slotPriceStep);
        this.slots = parseSlots(slots);
    }

    /**
     * Runs one generation pass. Safe to call concurrently and repeatedly.
     */
    public GenerationReport generateUpcomingShows() {

        leaseService.ensureLeaseRow();

        String owner = instanceId();

        if (!leaseService.tryAcquire(owner)) {
            log.info("Show generation skipped: another instance holds the lease");
            return GenerationReport.leaseNotAcquired();
        }

        try {
            return generate();
        } finally {
            leaseService.release(owner);
        }
    }

    /**
     * Runs one generation pass without touching the lease.
     *
     * <p>Used by the daily automation orchestrator, which acquires the lease
     * once and then runs the catalogue sync, the lifecycle evaluation and the
     * show generation under that single lease. Calling
     * {@link #generateUpcomingShows()} from there would fail to acquire the
     * lease a second time and silently skip generation, so this entry point
     * exists to avoid that.</p>
     */
    public GenerationReport generateUpcomingShowsWithLeaseHeld() {
        return generate();
    }

    private GenerationReport generate() {

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        LocalDateTime earliestStart = now.plusMinutes(minLeadMinutes);

        List<Movie> movies = movieRepository.findEligibleForShows(today);
        List<Theatre> theatres = theatreRepository.findEligibleForShows();

        if (movies.isEmpty() || theatres.isEmpty()) {

            log.info(
                    "Show generation: {} eligible movie(s), {} eligible theatre(s) - nothing to do",
                    movies.size(),
                    theatres.size());

            return new GenerationReport(true, 0, 0, 0, "no-eligible-data");
        }

        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (int day = 0; day < horizonDays; day++) {

            LocalDate date = today.plusDays(day);

            for (int theatreIndex = 0; theatreIndex < theatres.size(); theatreIndex++) {

                Theatre theatre = theatres.get(theatreIndex);
                List<Movie> rotation = rotationFor(movies, date, theatreIndex);

                for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {

                    LocalDateTime showTime =
                            LocalDateTime.of(date, slots.get(slotIndex));

                    // Never schedule a show that is already at or past its lead time.
                    if (!showTime.isAfter(earliestStart)) {
                        skipped++;
                        continue;
                    }

                    // One screen per theatre: never overlap an existing show.
                    if (showRepository.existsByTheatreIdAndShowTime(
                            theatre.getId(),
                            showTime)) {
                        skipped++;
                        continue;
                    }

                    Movie movie = rotation.get(slotIndex % rotation.size());
                    double price = baseTicketPrice + (slotIndex * slotPriceStep);

                    try {

                        Show savedShow = showService.addShowForScheduler(
                                movie,
                                theatre,
                                showTime,
                                price);

                        if (savedShow == null) {
                            skipped++;
                        } else {
                            created++;
                        }

                    } catch (DataIntegrityViolationException ex) {

                        // A concurrent instance inserted the same instant first.
                        skipped++;

                        log.debug(
                                "Skipping concurrently created show: movie={}, theatre={}, time={}",
                                movie.getId(),
                                theatre.getId(),
                                showTime);

                    } catch (RuntimeException ex) {

                        failed++;

                        log.warn(
                                "Failed to generate show: movie={}, theatre={}, time={}: {}",
                                movie.getId(),
                                theatre.getId(),
                                showTime,
                                ex.getMessage());
                    }
                }
            }
        }

        log.info(
                "Show generation finished: created={}, skipped={}, failed={}",
                created,
                skipped,
                failed);

        return new GenerationReport(true, created, skipped, failed, "completed");
    }

    /**
     * Picks a deterministic window of movies so the catalogue rotates between
     * days and theatres while a rerun for the same day reproduces exactly the
     * same plan.
     */
    private List<Movie> rotationFor(
            List<Movie> movies,
            LocalDate date,
            int theatreIndex) {

        int size = movies.size();
        int window = Math.min(moviesPerTheatrePerDay, size);

        int offset = Math.floorMod(
                (date.getDayOfYear() * 31) + (theatreIndex * 17),
                size);

        List<Movie> rotation = new ArrayList<>(window);

        for (int i = 0; i < window; i++) {
            rotation.add(movies.get((offset + i) % size));
        }

        return rotation;
    }

    private static List<LocalTime> parseSlots(String raw) {

        if (raw == null || raw.isBlank()) {
            return DEFAULT_SLOTS;
        }

        List<LocalTime> parsed = new ArrayList<>();

        for (String part : raw.split(",")) {

            String value = part.trim();

            if (value.isEmpty()) {
                continue;
            }

            try {
                parsed.add(LocalTime.parse(value));
            } catch (DateTimeParseException ex) {
                log.warn("Ignoring invalid show slot '{}'", value);
            }
        }

        if (parsed.isEmpty()) {
            return DEFAULT_SLOTS;
        }

        parsed.sort(Comparator.naturalOrder());

        return List.copyOf(parsed);
    }

    private static String instanceId() {
        return "cinepass-" + UUID.randomUUID();
    }

    /**
     * Outcome of a generation pass. {@code executed=false} means the pass did
     * not run (disabled or another instance held the lease).
     */
    public record GenerationReport(
            boolean executed,
            int created,
            int skipped,
            int failed,
            String reason) {

        public static GenerationReport leaseNotAcquired() {
            return new GenerationReport(false, 0, 0, 0, "lease-not-acquired");
        }
    }
}
