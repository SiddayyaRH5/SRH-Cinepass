package com.srh.cinepass;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieSource;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.service.MovieCatalogueSyncService;
import com.srh.cinepass.service.MovieCatalogueWriter;
import com.srh.cinepass.service.TmdbService;
import com.srh.cinepass.support.TicketTestSupport;
import com.srh.cinepass.support.TmdbStubServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Catalogue synchronisation against a local TMDB stub.
 *
 * <p>The real TMDB service is never contacted. The tests cover the import of
 * a new movie, the idempotent update of an existing movie, the protection of
 * manually managed movies, the refusal to import a movie without an Indian
 * theatrical release date, and the protection against duplicates when two
 * synchronisations run at the same time.</p>
 */
class MovieCatalogueSyncServiceTests extends TicketTestSupport {

    private static final String NOW_PLAYING = "/3/movie/now_playing";
    private static final String UPCOMING = "/3/movie/upcoming";

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 11, 15);

    private static TmdbStubServer server;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieCatalogueWriter catalogueWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MovieCatalogueSyncService syncService;

    @BeforeAll
    static void startStubServer() throws IOException {
        server = new TmdbStubServer();
    }

    @AfterAll
    static void stopStubServer() {
        server.close();
    }

    @BeforeEach
    void setUp() {
        testClock.set(LocalDateTime.of(2026, 11, 15, 2, 0));

        // A fresh client per test, so the response cache never leaks between
        // the tests.
        TmdbService tmdbService = new TmdbService(
                movieRepository,
                "test-access-token",
                server.baseUrl(),
                "IN",
                "en-IN",
                1000,
                2000,
                3,
                30,
                360);

        syncService = new MovieCatalogueSyncService(
                tmdbService,
                catalogueWriter,
                movieRepository,
                testClock,
                true,
                90,
                180);
    }

    @Test
    void importsAMovieThatHasAnIndianTheatricalReleaseDate() {

        givenDiscovery("9001");
        givenDetails("9001", "Synced Indian Movie", "2026-11-01", "IN");

        MovieCatalogueSyncService.SyncReport report = syncService.synchronize();

        assertThat(report.inserted()).isEqualTo(1);
        assertThat(report.updated()).isZero();
        assertThat(report.discovered()).isEqualTo(1);

        List<Movie> movies = movieRepository.findAll();

        assertThat(movies).hasSize(1);

        Movie movie = movies.get(0);

        assertThat(movie.getTmdbId()).isEqualTo(9001L);
        assertThat(movie.getTitle()).isEqualTo("Synced Indian Movie");
        assertThat(movie.getSource()).isEqualTo(MovieSource.TMDB);
        assertThat(movie.getStatus()).isEqualTo(MovieStatus.ACTIVE);
        assertThat(movie.getIndiaTheatricalReleaseDate())
                .isEqualTo(LocalDate.of(2026, 11, 1));

        // The effective release date follows the Indian theatrical date.
        assertThat(movie.getReleaseDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(movie.getTmdbSyncedAt()).isNotNull();
        assertThat(movie.getLastSyncError()).isNull();
    }

    @Test
    void updatesTheExistingMovieInsteadOfCreatingASecondOne() {

        givenDiscovery("9001");
        givenDetails("9001", "Synced Indian Movie", "2026-11-01", "IN");

        syncService.synchronize();

        // The same movie is returned with refreshed metadata.
        server.respond("/3/movie/9001", """
                {"id":9001,"title":"Renamed Indian Movie","overview":"New synopsis",
                 "poster_path":"/new.jpg","backdrop_path":"/newb.jpg",
                 "original_language":"hi","vote_average":8.1,"runtime":150,
                 "genres":[{"id":1,"name":"Drama"}],
                 "release_dates":{"id":9001,"results":[
                   {"iso_3166_1":"IN","release_dates":[
                      {"type":3,"release_date":"2026-11-01T00:00:00.000Z"}]}]}}
                """);

        // A fresh client, so the details are read again.
        setUp();

        MovieCatalogueSyncService.SyncReport report = syncService.synchronize();

        assertThat(report.inserted()).isZero();
        assertThat(report.updated()).isEqualTo(1);
        assertThat(movieRepository.findAll()).hasSize(1);

        Movie movie = movieRepository.findByTmdbId(9001L).orElseThrow();

        assertThat(movie.getTitle()).isEqualTo("Renamed Indian Movie");
        assertThat(movie.getDescription()).isEqualTo("New synopsis");
        assertThat(movie.getRating()).isEqualTo(8.1);
        assertThat(movie.getDuration()).isEqualTo(150);
    }

    @Test
    void doesNotImportAMovieWithoutAnIndianTheatricalReleaseDate() {

        givenDiscovery("9002");
        givenDetails("9002", "Foreign Movie", "2026-11-01", "US");

        MovieCatalogueSyncService.SyncReport report = syncService.synchronize();

        assertThat(report.inserted()).isZero();
        assertThat(report.skipped()).isEqualTo(1);
        assertThat(movieRepository.findAll()).isEmpty();
    }

    @Test
    void neverOverwritesAManuallyManagedMovie() {

        Movie manual = new Movie();
        manual.setTmdbId(9003L);
        manual.setTitle("Hand written title");
        manual.setSource(MovieSource.MANUAL);
        manual.setStatus(MovieStatus.ACTIVE);
        manualRepositorySave(manual);

        givenDiscovery("9003");
        givenDetails("9003", "TMDB Title", "2026-11-01", "IN");

        MovieCatalogueSyncService.SyncReport report = syncService.synchronize();

        assertThat(report.inserted()).isZero();
        assertThat(report.updated()).isZero();
        assertThat(report.skipped()).isEqualTo(1);

        Movie stored = movieRepository.findByTmdbId(9003L).orElseThrow();

        assertThat(stored.getTitle()).isEqualTo("Hand written title");
        assertThat(stored.getSource()).isEqualTo(MovieSource.MANUAL);
    }

    @Test
    void doesNotImportAMovieWhoseIndianDateIsOutsideTheDiscoveryWindow() {

        givenDiscovery("9004");
        givenDetails("9004", "Far Future Movie", "2027-11-01", "IN");

        MovieCatalogueSyncService.SyncReport report = syncService.synchronize();

        assertThat(report.inserted()).isZero();
        assertThat(movieRepository.findAll()).isEmpty();
    }

    @Test
    void reportsAnOutageWithoutCorruptingTheCatalogue() {

        server.respondWithStatus(NOW_PLAYING, 503, "{}");
        server.respondWithStatus(UPCOMING, 503, "{}");

        MovieCatalogueSyncService.SyncReport report = syncService.synchronize();

        assertThat(report.inserted()).isZero();
        assertThat(report.hasErrors()).isTrue();
        assertThat(movieRepository.findAll()).isEmpty();
    }

    @Test
    void concurrentSynchronisationsNeverCreateADuplicateMovie() throws Exception {

        // The production schema protects the TMDB id with a unique index.
        // The H2 test schema is built from the entity, so the index is
        // created here to exercise the same protection.
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_movie_tmdb_id ON movie (tmdb_id)");

        try {
            givenDiscovery("9005");
            givenDetails("9005", "Concurrent Movie", "2026-11-01", "IN");

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);

            try {
                Future<MovieCatalogueSyncService.SyncReport> first =
                        executor.submit(() -> {
                            start.await(10, TimeUnit.SECONDS);
                            return syncService.synchronize();
                        });

                Future<MovieCatalogueSyncService.SyncReport> second =
                        executor.submit(() -> {
                            start.await(10, TimeUnit.SECONDS);
                            return syncService.synchronize();
                        });

                start.countDown();

                first.get(30, TimeUnit.SECONDS);
                second.get(30, TimeUnit.SECONDS);

            } finally {
                executor.shutdownNow();
            }

            List<Movie> movies = movieRepository.findAll()
                    .stream()
                    .filter(movie -> Long.valueOf(9005L).equals(movie.getTmdbId()))
                    .toList();

            assertThat(movies).hasSize(1);
            assertThat(movieRepository.findAll()).hasSize(1);

        } finally {
            jdbcTemplate.execute("DROP INDEX uk_movie_tmdb_id");
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void manualRepositorySave(Movie movie) {
        movieRepository.saveAndFlush(movie);
    }

    private void givenDiscovery(String tmdbId) {

        server.respond(NOW_PLAYING, """
                {"page":1,"total_pages":1,"results":[
                  {"id":%s,"title":"Candidate","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"hi","vote_average":7.0}]}
                """.formatted(tmdbId));

        server.respond(UPCOMING, """
                {"page":1,"total_pages":1,"results":[]}
                """);
    }

    private void givenDetails(String tmdbId, String title, String date, String country) {

        server.respond("/3/movie/" + tmdbId, """
                {"id":%s,"title":"%s","overview":"Synopsis",
                 "poster_path":"/p.jpg","backdrop_path":"/b.jpg",
                 "original_language":"hi","vote_average":7.0,"runtime":140,
                 "genres":[{"id":1,"name":"Drama"}],
                 "release_dates":{"id":%s,"results":[
                   {"iso_3166_1":"%s","release_dates":[
                      {"type":3,"release_date":"%sT00:00:00.000Z"}]}]}}
                """.formatted(tmdbId, title, tmdbId, country, date));
    }
}
