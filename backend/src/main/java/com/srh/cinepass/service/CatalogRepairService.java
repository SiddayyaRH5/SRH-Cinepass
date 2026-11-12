package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.repository.ShowRepository;
import com.srh.cinepass.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class CatalogRepairService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final TheatreRepository theatreRepository;
    private final ShowService showService;
    private final TmdbService tmdbService;

    private static final List<Long> MOVIE_IDS = List.of(
        3L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L,
        15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L
    );

    private static final Map<String, List<Long>> CITY_THEATRES = Map.of(
        "Bengaluru", List.of(1L, 2L, 6L, 7L, 8L, 9L),
        "Chennai", List.of(5L, 18L, 19L, 20L, 21L, 22L),
        "Hyderabad", List.of(4L, 13L, 14L, 15L, 16L, 17L),
        "Mysuru", List.of(3L, 10L, 11L, 12L),
        "Mumbai", List.of(23L, 24L, 25L, 26L, 27L)
    );

    private static final List<LocalTime> TIMES = List.of(
        LocalTime.of(10, 0),
        LocalTime.of(14, 0),
        LocalTime.of(18, 0),
        LocalTime.of(21, 0)
    );

    public CatalogRepairService(
        MovieRepository movieRepository,
        ShowRepository showRepository,
        TheatreRepository theatreRepository,
        ShowService showService,
        TmdbService tmdbService
    ) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.theatreRepository = theatreRepository;
        this.showService = showService;
        this.tmdbService = tmdbService;
    }

    @Transactional
    public Map<String, Object> repairCatalog() {
        int postersUpdated = 0;
        int showsCreated = 0;
        int seatsCreated = 0;
        int moviesSkipped = 0;
        int posterFailures = 0;

        for (Long id : List.of(37L, 44L, 52L, 53L, 54L, 55L)) {
            Movie movie = movieRepository.findById(id).orElse(null);
            if (movie != null) {
                try {
                    if (tmdbService.fetchAndUpdateMovieMetadata(movie)) {
                        postersUpdated++;
                    } else {
                        posterFailures++;
                    }
                } catch (Exception ex) {
                    posterFailures++;
                }
            }
        }

        int movieIndex = 0;
        for (Long movieId : MOVIE_IDS) {
            Movie movie = movieRepository.findById(movieId).orElse(null);

            if (movie == null || !showRepository.findByMovieId(movieId).isEmpty()) {
                moviesSkipped++;
                movieIndex++;
                continue;
            }

            LocalDate releaseDate = movie.getReleaseDate();
            LocalDate firstDate = LocalDate.now().plusDays(1);

            if (releaseDate != null && releaseDate.isAfter(firstDate)) {
                firstDate = releaseDate;
            }

            for (List<Long> theatreIds : CITY_THEATRES.values()) {
                int theatreCount = theatreIds.size();
                int theatreIndex = movieIndex % theatreCount;
                int timeIndex = (movieIndex / theatreCount) % TIMES.size();
                int dayOffset = movieIndex / (theatreCount * TIMES.size());

                Theatre theatre = theatreRepository.findById(
                    theatreIds.get(theatreIndex)
                ).orElse(null);

                if (theatre == null) {
                    continue;
                }

                Show show = new Show();
                show.setMovie(movie);
                show.setTheatre(theatre);
                show.setShowDate(firstDate.plusDays(dayOffset));
                show.setShowTime(TIMES.get(timeIndex));
                show.setTicketPrice(180.0);

                showService.addShow(show, null, true);
                showsCreated++;
                seatsCreated += theatre.getTotalSeats() == null
                    ? 0 : theatre.getTotalSeats();
            }

            movieIndex++;
        }

        return Map.of(
            "success", true,
            "postersUpdated", postersUpdated,
            "posterFailures", posterFailures,
            "showsCreated", showsCreated,
            "seatsExpected", seatsCreated,
            "moviesSkipped", moviesSkipped
        );
    }
}

