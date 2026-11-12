
package com.srh.cinepass.seed;

import com.srh.cinepass.entity.*;
import com.srh.cinepass.repository.*;
import com.srh.cinepass.service.MovieService;
import com.srh.cinepass.service.TheatreService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@ConditionalOnProperty(
    prefix = "demo.seed",
    name = "enabled",
    havingValue = "true"
)
public class DemoDataSeeder implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final LocationRepository locationRepository;
    private final TheatreRepository theatreRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final MovieService movieService;
    private final TheatreService theatreService;

    public DemoDataSeeder(
            MovieRepository movieRepository,
            LocationRepository locationRepository,
            TheatreRepository theatreRepository,
            ShowRepository showRepository,
            SeatRepository seatRepository,
            MovieService movieService,
            TheatreService theatreService) {

        this.movieRepository = movieRepository;
        this.locationRepository = locationRepository;
        this.theatreRepository = theatreRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.movieService = movieService;
        this.theatreService = theatreService;
    }

    private record MovieData(
        long tmdbId,
        String title,
        String description,
        String poster,
        String release,
        double rating,
        int duration,
        String language,
        String genre
    ) {}

    private record TheatreData(String city, String name) {}

    @Override
    public void run(String... args) {

        System.out.println("==================================");
        System.out.println("SRH CINEPASS DEMO SEEDER STARTED");
        System.out.println("==================================");

        List<Movie> movies = seedMovies();
        Map<String, Location> locations = seedLocations();
        List<Theatre> theatres = seedTheatres(locations);

        seedShows(movies, theatres);

        System.out.println("==================================");
        System.out.println("DEMO SEEDING COMPLETED");
        System.out.println("Movies: " + movies.size());
        System.out.println("Theatres: " + theatres.size());
        System.out.println("==================================");
    }

    // =========================================
    // MOVIES
    // =========================================

    private List<Movie> seedMovies() {

        List<MovieData> data = List.of(

            new MovieData(27205L, "Inception",
                "A skilled thief enters dreams to steal secrets.",
                "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
                "2010-07-15", 8.8, 148, "English", "Sci-Fi, Action"),

            new MovieData(157336L, "Interstellar",
                "A team travels beyond this galaxy to save humanity.",
                "/gEU2QniE6E77NI6lCU6MctlT1vI.jpg",
                "2014-11-05", 8.7, 169, "English", "Sci-Fi, Drama"),

            new MovieData(155L, "The Dark Knight",
                "Batman faces the Joker in Gotham City.",
                "/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
                "2008-07-16", 9.0, 152, "English", "Action, Crime"),

            new MovieData(19995L, "Avatar",
                "A marine discovers a new world on Pandora.",
                "/kyeqWdyUXW608qlYkRqosgbbJyK.jpg",
                "2009-12-18", 7.9, 162, "English", "Sci-Fi, Adventure"),

            new MovieData(299534L, "Avengers Endgame",
                "The Avengers unite for one final battle.",
                "/or06FN3Dka5tukK1e9sl16pB3iy.jpg",
                "2019-04-26", 8.4, 181, "English", "Action, Adventure"),

            new MovieData(634649L, "Spider-Man No Way Home",
                "Peter Parker faces a multiverse of enemies.",
                "/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg",
                "2021-12-17", 8.0, 148, "English", "Action, Adventure"),

            new MovieData(693134L, "Dune Part Two",
                "Paul Atreides continues his journey on Arrakis.",
                "/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
                "2024-03-01", 8.5, 166, "English", "Sci-Fi, Adventure"),

            new MovieData(872585L, "Oppenheimer",
                "The story of the scientist behind the atomic bomb.",
                "/ptpr0kGAckfQkJeJIt8st5dglvd.jpg",
                "2023-07-21", 8.3, 180, "English", "Drama, History"),

            new MovieData(579974L, "RRR",
                "Two revolutionaries fight for freedom in India.",
                "/nEufeZlyAOLqO2brrs0yeF1lgXX.jpg",
                "2022-03-25", 7.8, 187, "Telugu", "Action, Drama"),

            new MovieData(587412L, "KGF Chapter 2",
                "Rocky faces powerful enemies in the Kolar Gold Fields.",
                "/khNVygolU0TxLIDWff5tQlAhZ23.jpg",
                "2022-04-14", 8.2, 168, "Kannada", "Action, Crime"),

            new MovieData(20453L, "3 Idiots",
                "Three engineering students challenge the education system.",
                "/66A9MqXOyVFCssoloscw79z8Tew.jpg",
                "2009-12-25", 8.4, 170, "Hindi", "Comedy, Drama"),

            new MovieData(475557L, "Joker",
                "A troubled comedian descends into madness.",
                "/udDclJoHjfjb8Ekgsd4FDteOkCU.jpg",
                "2019-10-04", 8.2, 122, "English", "Crime, Drama"),

            new MovieData(361743L, "Top Gun Maverick",
                "Maverick trains a new generation of pilots.",
                "/62HCnUTziyWcpDaBO2i1DX17ljH.jpg",
                "2022-05-27", 8.2, 130, "English", "Action, Drama"),

            new MovieData(414906L, "The Batman",
                "Batman investigates corruption in Gotham.",
                "/74xTEgt7R36Fpooo50r9T25onhq.jpg",
                "2022-03-04", 7.8, 176, "English", "Action, Crime"),

            new MovieData(354912L, "Coco",
                "A young boy discovers his family's musical history.",
                "/gGEsBPAijhVUFoiNpgZXqRVWJt2.jpg",
                "2017-11-22", 8.4, 105, "English", "Animation, Family"),

            new MovieData(330457L, "Frozen II",
                "Elsa and Anna discover the secrets of their kingdom.",
                "/mINJaa34MtknCYl5AjtNJzWj8cD.jpg",
                "2019-11-22", 7.2, 103, "English", "Animation, Fantasy"),

            new MovieData(360814L, "Dangal",
                "A former wrestler trains his daughters to compete.",
                "/cJRPOLE3Gz4jTq0gkldC8Wn7aql.jpg",
                "2016-12-23", 8.3, 161, "Hindi", "Drama, Sport"),

            new MovieData(1139087L, "Drishyam 2",
                "A family faces a renewed investigation.",
                "/pJQn9Z7t7J9x.jpg",
                "2022-11-18", 8.0, 140, "Hindi", "Thriller, Crime"),

            new MovieData(872906L, "Jawan",
                "A man takes on corruption and injustice.",
                "/jFt1gS4BGHs88W5Q4Zx1f5wJz7x.jpg",
                "2023-09-07", 7.0, 169, "Hindi", "Action, Thriller"),

            new MovieData(1049948L, "Kantara",
                "A village mystery unfolds around ancient traditions.",
                "/k7VJqX7f.jpg",
                "2022-09-30", 8.2, 148, "Kannada", "Action, Thriller")
        );

        List<Movie> result = new ArrayList<>();
        List<Movie> existing = movieRepository.findAll();

        for (MovieData item : data) {

            Movie movie = existing.stream()
                .filter(m -> m.getTitle() != null &&
                    m.getTitle().equalsIgnoreCase(item.title()))
                .findFirst()
                .orElse(null);

            if (movie == null) {

                movie = new Movie();
                movie.setTmdbId(item.tmdbId());
                movie.setTitle(item.title());
                movie.setDescription(item.description());

                movie.setPosterUrl(
                    "https://image.tmdb.org/t/p/w500" + item.poster()
                );

                movie.setBackdropUrl(
                    "https://image.tmdb.org/t/p/w1280" + item.poster()
                );

                movie.setReleaseDate(LocalDate.parse(item.release()));
                movie.setRating(item.rating());
                movie.setDuration(item.duration());
                movie.setLanguage(item.language());
                movie.setGenre(item.genre());

                movie = movieService.addMovie(movie);
                existing.add(movie);

                System.out.println("Added movie: " + movie.getTitle());

            } else {
                System.out.println("Existing movie: " + movie.getTitle());
            }

            result.add(movie);
        }

        return result;
    }

    // =========================================
    // LOCATIONS
    // =========================================

    private Map<String, Location> seedLocations() {

        Map<String, Location> result = new HashMap<>();

        String[][] cities = {
            {"Bengaluru", "Karnataka"},
            {"Mysuru", "Karnataka"},
            {"Hyderabad", "Telangana"},
            {"Chennai", "Tamil Nadu"},
            {"Mumbai", "Maharashtra"}
        };

        List<Location> existing = locationRepository.findAll();

        for (String[] city : cities) {

            Location location = existing.stream()
                .filter(l -> l.getCity().equalsIgnoreCase(city[0]))
                .findFirst()
                .orElse(null);

            if (location == null) {

                location = locationRepository.save(
                    new Location(city[0], city[1], "India")
                );

                System.out.println("Added city: " + city[0]);
            }

            result.put(city[0].toLowerCase(Locale.ROOT), location);
        }

        return result;
    }

    // =========================================
    // THEATRES
    // =========================================

    private List<Theatre> seedTheatres(
            Map<String, Location> locations) {

        List<TheatreData> data = List.of(

            new TheatreData("Bengaluru", "PVR Orion Mall"),
            new TheatreData("Bengaluru", "INOX Garuda Mall"),
            new TheatreData("Bengaluru", "Cinepolis Royal Meenakshi"),
            new TheatreData("Bengaluru", "PVR Nexus Koramangala"),
            new TheatreData("Bengaluru", "INOX Mantri Square"),

            new TheatreData("Mysuru", "DRC Cinemas"),
            new TheatreData("Mysuru", "INOX Mall of Mysore"),
            new TheatreData("Mysuru", "PVR Forum Centre City"),

            new TheatreData("Hyderabad", "PVR Prasads"),
            new TheatreData("Hyderabad", "AMB Cinemas"),
            new TheatreData("Hyderabad", "PVR Next Galleria"),
            new TheatreData("Hyderabad", "Asian Cinemas"),
            new TheatreData("Hyderabad", "Cinepolis Manjeera"),

            new TheatreData("Chennai", "PVR VR Chennai"),
            new TheatreData("Chennai", "AGS Cinemas"),
            new TheatreData("Chennai", "Sathyam Cinemas"),
            new TheatreData("Chennai", "Luxe Cinemas"),
            new TheatreData("Chennai", "SPI Palazzo"),

            new TheatreData("Mumbai", "PVR ICON Phoenix"),
            new TheatreData("Mumbai", "INOX Megaplex"),
            new TheatreData("Mumbai", "Cinepolis Andheri"),
            new TheatreData("Mumbai", "PVR Juhu"),
            new TheatreData("Mumbai", "INOX Nariman Point")
        );

        List<Theatre> result = new ArrayList<>();
        List<Theatre> existing = theatreRepository.findAll();

        for (TheatreData item : data) {

            Location location = locations.get(
                item.city().toLowerCase(Locale.ROOT)
            );

            Theatre theatre = existing.stream()
                .filter(t -> t.getName().equalsIgnoreCase(item.name())
                    && t.getLocation() != null
                    && t.getLocation().getId().equals(location.getId()))
                .findFirst()
                .orElse(null);

            if (theatre == null) {

                theatre = new Theatre(item.name(), 120, location);
                theatre = theatreService.addTheatre(theatre, null);

                existing.add(theatre);

                System.out.println("Added theatre: " + item.name());
            }

            result.add(theatre);
        }

        return result;
    }

    // =========================================
    // SHOWS AND SEATS
    // =========================================

    private void seedShows(
            List<Movie> movies,
            List<Theatre> theatres) {

        LocalDate firstDate = LocalDate.now().plusDays(1);

        LocalTime[] timings = {
            LocalTime.of(10, 0),
            LocalTime.of(13, 30),
            LocalTime.of(17, 0),
            LocalTime.of(20, 30)
        };

        Map<String, Show> existingShows = new HashMap<>();

        for (Show existing : showRepository.findAll()) {

            if (existing.getMovie() != null &&
                existing.getTheatre() != null &&
                existing.getShowDate() != null &&
                existing.getShowTime() != null) {

                String key = showKey(
                    existing.getMovie().getId(),
                    existing.getTheatre().getId(),
                    LocalDateTime.of(
                        existing.getShowDate(),
                        existing.getShowTime()
                    )
                );

                existingShows.put(key, existing);
            }
        }

        int createdShows = 0;
        int createdSeats = 0;

        for (int t = 0; t < theatres.size(); t++) {

            Theatre theatre = theatres.get(t);

            for (int day = 0; day < 3; day++) {

                LocalDate date = firstDate.plusDays(day);

                for (int slot = 0; slot < timings.length; slot++) {

                    Movie movie = movies.get(
                        (t * 4 + day * 4 + slot) % movies.size()
                    );

                    LocalDateTime dateTime =
                        LocalDateTime.of(date, timings[slot]);

                    String key = showKey(
                        movie.getId(),
                        theatre.getId(),
                        dateTime
                    );

                    Show savedShow = existingShows.get(key);

                    if (savedShow == null) {

                        Show show = new Show();
                        show.setMovie(movie);
                        show.setTheatre(theatre);
                        show.setShowTime(dateTime);
                        show.setTicketPrice(180.0 + slot * 30.0);

                        savedShow = showRepository.save(show);

                        existingShows.put(key, savedShow);
                        createdShows++;

                        System.out.println(
                            "Added show: " + movie.getTitle()
                            + " | " + theatre.getName()
                        );
                    }

                    // Get existing seats without changing their state.
                    List<Seat> existingSeats =
                        seatRepository.findByShowId(savedShow.getId());

                    Set<String> existingSeatNumbers = new HashSet<>();

                    for (Seat seat : existingSeats) {
                        existingSeatNumbers.add(seat.getSeatNumber());
                    }

                    List<Seat> missingSeats = new ArrayList<>();

                    for (int i = 1; i <= theatre.getTotalSeats(); i++) {

                        String seatNumber = "S" + i;

                        if (!existingSeatNumbers.contains(seatNumber)) {

                            Seat seat = new Seat();
                            seat.setSeatNumber(seatNumber);
                            seat.setBooked(false);
                            seat.setShow(savedShow);

                            missingSeats.add(seat);
                        }
                    }

                    if (!missingSeats.isEmpty()) {

                        saveSeatsInBatches(missingSeats);

                        createdSeats += missingSeats.size();

                        System.out.println(
                            "Added " + missingSeats.size()
                            + " missing seats for show ID: "
                            + savedShow.getId()
                        );
                    }
                }
            }
        }

        System.out.println("----------------------------------");
        System.out.println("New shows: " + createdShows);
        System.out.println("New seats: " + createdSeats);
        System.out.println("----------------------------------");
    }

    // =========================================
    // BATCH SEAT INSERTION
    // =========================================

    private void saveSeatsInBatches(List<Seat> seats) {

        final int batchSize = 50;

        for (int start = 0; start < seats.size(); start += batchSize) {

            int end = Math.min(start + batchSize, seats.size());

            List<Seat> batch = seats.subList(start, end);

            seatRepository.saveAll(batch);
            seatRepository.flush();
        }
    }

    // =========================================
    // SHOW KEY
    // =========================================

    private String showKey(
            Long movieId,
            Long theatreId,
            LocalDateTime dateTime) {

        return movieId + "|" + theatreId + "|" + dateTime;
    }
}
