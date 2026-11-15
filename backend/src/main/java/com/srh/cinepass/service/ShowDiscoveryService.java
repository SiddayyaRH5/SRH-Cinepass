package com.srh.cinepass.service;

import com.srh.cinepass.dto.ShowAvailabilityResponse;
import com.srh.cinepass.dto.ShowCityResponse;
import com.srh.cinepass.dto.ShowDateResponse;
import com.srh.cinepass.dto.ShowTheatreResponse;
import com.srh.cinepass.dto.ShowTimeResponse;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;
import com.srh.cinepass.repository.SeatRepository;
import com.srh.cinepass.repository.ShowRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only movie discovery: which cities, theatres, dates and showtimes still
 * have bookable shows, plus live seat availability.
 *
 * <p>All heavy filtering happens in {@link ShowRepository} JPQL queries. This
 * service only maps rows to DTOs and aggregates seat counts, so no request ever
 * loads the whole show table into memory.</p>
 */
@Service
@Transactional(readOnly = true)
public class ShowDiscoveryService {

    private static final String APPROVED = "APPROVED";

    /**
     * Defensive cap so a pathological data set cannot return an unbounded list
     * of showtimes for one theatre and date.
     */
    private static final int MAX_SHOWTIMES_PER_QUERY = 100;

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final ShowAvailabilityPolicy availabilityPolicy;
    private final Clock clock;

    public ShowDiscoveryService(
            ShowRepository showRepository,
            SeatRepository seatRepository,
            ShowAvailabilityPolicy availabilityPolicy,
            Clock clock) {

        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.availabilityPolicy = availabilityPolicy;
        this.clock = clock;
    }

    public List<ShowCityResponse> getCitiesForMovie(Long movieId) {

        return showRepository
                .findCitiesWithAvailableShows(
                        movieId,
                        bookableFrom(),
                        ShowStatus.SCHEDULED,
                        APPROVED)
                .stream()
                .map(ShowCityResponse::from)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ShowTheatreResponse> getTheatresForMovieInCity(
            Long movieId,
            Long locationId) {

        return showRepository
                .findTheatresWithAvailableShows(
                        movieId,
                        locationId,
                        bookableFrom(),
                        ShowStatus.SCHEDULED,
                        APPROVED)
                .stream()
                .map(ShowTheatreResponse::from)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ShowDateResponse> getDatesForMovieAtTheatre(
            Long movieId,
            Long theatreId) {

        return showRepository
                .findAvailableShowInstants(
                        movieId,
                        theatreId,
                        bookableFrom(),
                        ShowStatus.SCHEDULED,
                        APPROVED)
                .stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .map(ShowDateResponse::new)
                .toList();
    }

    public List<ShowTimeResponse> getShowTimes(
            Long movieId,
            Long theatreId,
            LocalDate date) {

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        LocalDateTime from = max(bookableFrom(), dayStart);

        List<Show> shows = showRepository.findAvailableShows(
                movieId,
                theatreId,
                from,
                dayEnd,
                ShowStatus.SCHEDULED,
                APPROVED);

        if (shows.size() > MAX_SHOWTIMES_PER_QUERY) {
            shows = shows.subList(0, MAX_SHOWTIMES_PER_QUERY);
        }

        Map<Long, int[]> availability = loadAvailability(shows);

        List<ShowTimeResponse> result = new ArrayList<>(shows.size());

        for (Show show : shows) {

            int[] counts = availability.getOrDefault(
                    show.getId(),
                    new int[] { 0, 0 });

            int total = counts[0];
            int unavailable = counts[1];

            result.add(new ShowTimeResponse(
                    show.getId(),
                    show.getShowDate(),
                    show.getShowTime(),
                    show.getTicketPrice(),
                    total,
                    Math.max(0, total - unavailable)));
        }

        return result;
    }

    public ShowAvailabilityResponse getAvailability(Long showId) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));

        LocalDateTime now = LocalDateTime.now(clock);

        int total = (int) seatRepository.countByShowId(showId);
        int unavailable = 0;

        List<Object[]> rows = seatRepository.countAvailability(
                List.of(showId),
                now);

        if (!rows.isEmpty()) {

            Object[] row = rows.get(0);

            total = number(row[1]);
            unavailable = number(row[2]);
        }

        String status = show.getStatus() == null
                ? ShowStatus.SCHEDULED.name()
                : show.getStatus().name();

        return new ShowAvailabilityResponse(
                show.getId(),
                show.getShowDate(),
                show.getShowTime(),
                show.getTicketPrice(),
                total,
                Math.max(0, total - unavailable),
                availabilityPolicy.isBookable(show, now),
                status);
    }

    private Map<Long, int[]> loadAvailability(List<Show> shows) {

        if (shows.isEmpty()) {
            return Map.of();
        }

        List<Long> showIds = shows.stream()
                .map(Show::getId)
                .toList();

        Map<Long, int[]> result = new HashMap<>();

        for (Object[] row : seatRepository.countAvailability(
                showIds,
                LocalDateTime.now(clock))) {

            result.put(
                    ((Number) row[0]).longValue(),
                    new int[] { number(row[1]), number(row[2]) });
        }

        return result;
    }

    private LocalDateTime bookableFrom() {
        return availabilityPolicy.earliestBookableStart(
                LocalDateTime.now(clock));
    }

    private static int number(Object value) {

        if (value == null) {
            return 0;
        }

        return ((Number) value).intValue();
    }

    private static LocalDateTime max(
            LocalDateTime first,
            LocalDateTime second) {

        return first.isAfter(second) ? first : second;
    }
}
