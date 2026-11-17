package com.srh.cinepass;

import com.srh.cinepass.dto.TmdbCastMember;
import com.srh.cinepass.dto.TmdbMovieCandidate;
import com.srh.cinepass.dto.TmdbMovieDetails;
import com.srh.cinepass.service.TmdbService;
import com.srh.cinepass.support.TmdbStubServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TMDB client behaviour against a local stub server.
 *
 * <p>The real TMDB service is never contacted. The tests cover the parsing of
 * the regional discovery lists, the Indian theatrical release date rules, the
 * pagination, the failure handling and the credential handling.</p>
 */
class TmdbServiceTests {

    private static final String NOW_PLAYING = "/3/movie/now_playing";
    private static final String UPCOMING = "/3/movie/upcoming";

    private TmdbStubServer server;
    private TmdbService tmdbService;

    @BeforeEach
    void setUp() throws IOException {
        server = new TmdbStubServer();
        tmdbService = service("test-access-token", 2, 2000);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    // ============================================================
    // Discovery
    // ============================================================

    @Test
    void readsUpcomingAndNowPlayingMoviesForTheIndianRegion() {

        server.respond(NOW_PLAYING, """
                {"page":1,"total_pages":1,"total_results":1,"results":[
                  {"id":101,"title":"Indian Movie","overview":"Synopsis",
                   "poster_path":"/p.jpg","backdrop_path":"/b.jpg",
                   "original_language":"hi","vote_average":7.5,
                   "release_date":"2026-01-10"}]}
                """);

        server.respond(UPCOMING, """
                {"page":1,"total_pages":1,"total_results":1,"results":[
                  {"id":202,"title":"Upcoming Indian","overview":"",
                   "poster_path":null,"backdrop_path":null,
                   "original_language":"en","vote_average":0}]}
                """);

        TmdbService.DiscoveryResult result =
                tmdbService.fetchDiscoveryCandidates();

        assertThat(result.candidates()).hasSize(2);
        assertThat(result.pagesProcessed()).isEqualTo(2);

        assertThat(result.candidates())
                .extracting(TmdbMovieCandidate::tmdbId)
                .containsExactly(101L, 202L);

        TmdbMovieCandidate first = result.candidates().get(0);

        assertThat(first.title()).isEqualTo("Indian Movie");
        assertThat(first.originalLanguage()).isEqualTo("hi");
        assertThat(first.posterPath()).isEqualTo("/p.jpg");

        // A null poster path must not turn into the literal string "null".
        TmdbMovieCandidate second = result.candidates().get(1);

        assertThat(second.posterPath()).isEmpty();
        assertThat(second.backdropPath()).isEmpty();

        // The Indian region is requested for both lists.
        assertThat(server.requests())
                .allSatisfy(request -> assertThat(request).contains("region=IN"));
    }

    @Test
    void stopsPagingAtTheConfiguredPageLimit() {

        server.respond(NOW_PLAYING, """
                {"page":1,"total_pages":9,"total_results":90,"results":[
                  {"id":1,"title":"Page one","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"en","vote_average":0}]}
                """);

        TmdbService.DiscoveryResult result =
                tmdbService.fetchDiscoveryCandidates();

        // Two pages for now_playing, and the upcoming list is empty.
        assertThat(result.pagesProcessed()).isEqualTo(2);

        long nowPlayingRequests = server.requests().stream()
                .filter(request -> request.startsWith(NOW_PLAYING))
                .count();

        assertThat(nowPlayingRequests).isEqualTo(2);
    }

    @Test
    void mergesDuplicateMoviesAcrossTheTwoLists() {

        server.respond(NOW_PLAYING, """
                {"page":1,"total_pages":1,"results":[
                  {"id":101,"title":"Indian Movie","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"hi","vote_average":7.5}]}
                """);

        server.respond(UPCOMING, """
                {"page":1,"total_pages":1,"results":[
                  {"id":101,"title":"Indian Movie","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"hi","vote_average":7.5}]}
                """);

        TmdbService.DiscoveryResult result =
                tmdbService.fetchDiscoveryCandidates();

        assertThat(result.candidates()).hasSize(1);
    }

    @Test
    void skipsCandidatesWithoutAUsableIdentifierOrTitle() {

        server.respond(NOW_PLAYING, """
                {"page":1,"total_pages":1,"results":[
                  {"id":0,"title":"No id","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"en","vote_average":0},
                  {"id":55,"title":"  ","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"en","vote_average":0},
                  {"id":77,"title":"Real Movie","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"en","vote_average":0}]}
                """);

        TmdbService.DiscoveryResult result =
                tmdbService.fetchDiscoveryCandidates();

        assertThat(result.candidates())
                .extracting(TmdbMovieCandidate::tmdbId)
                .containsExactly(77L);
    }

    @Test
    void reportsAnApiOutageWithoutFailingTheRun() {

        server.respondWithStatus(NOW_PLAYING, 503, "{\"error\":\"unavailable\"}");
        server.respondWithStatus(UPCOMING, 503, "{\"error\":\"unavailable\"}");

        TmdbService.DiscoveryResult result =
                tmdbService.fetchDiscoveryCandidates();

        assertThat(result.candidates()).isEmpty();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void doesNotCallTheApiWhenTheCredentialIsMissing() {

        TmdbService withoutToken = service("", 2, 2000);

        TmdbService.DiscoveryResult result =
                withoutToken.fetchDiscoveryCandidates();

        assertThat(result.candidates()).isEmpty();
        assertThat(result.pagesProcessed()).isZero();
        assertThat(result.errors()).isNotEmpty();
        assertThat(server.requests()).isEmpty();
    }

    // ============================================================
    // Details and the Indian theatrical release date
    // ============================================================

    @Test
    void readsTheMovieDetailsWithTheIndianTheatricalReleaseDate() {

        server.respond("/3/movie/101", """
                {"id":101,"title":"Indian Movie","overview":"Synopsis",
                 "poster_path":"/p.jpg","backdrop_path":"/b.jpg",
                 "original_language":"hi","vote_average":7.5,"runtime":148,
                 "genres":[{"id":1,"name":"Drama"},{"id":2,"name":"Action"}],
                 "release_dates":{"id":101,"results":[
                   {"iso_3166_1":"IN","release_dates":[
                      {"type":3,"release_date":"2026-01-10T00:00:00.000Z"}]},
                   {"iso_3166_1":"US","release_dates":[
                      {"type":3,"release_date":"2026-03-20T00:00:00.000Z"}]}]}}
                """);

        Optional<TmdbMovieDetails> details =
                tmdbService.fetchMovieDetails(101L);

        assertThat(details).isPresent();

        TmdbMovieDetails movie = details.get();

        assertThat(movie.tmdbId()).isEqualTo(101L);
        assertThat(movie.title()).isEqualTo("Indian Movie");
        assertThat(movie.genres()).isEqualTo("Drama, Action");
        assertThat(movie.runtime()).isEqualTo(148);
        assertThat(movie.rating()).isEqualTo(7.5);
        assertThat(movie.posterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/p.jpg");

        // The Indian date wins over the worldwide date.
        assertThat(movie.indiaTheatricalReleaseDate())
                .isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void ignoresDigitalStreamingPhysicalAndTvDates() {

        server.respond("/3/movie/102", """
                {"id":102,"title":"Streaming Movie","overview":"",
                 "poster_path":null,"backdrop_path":null,
                 "original_language":"en","vote_average":0,
                 "release_dates":{"id":102,"results":[
                   {"iso_3166_1":"IN","release_dates":[
                      {"type":4,"release_date":"2026-01-10T00:00:00.000Z"},
                      {"type":5,"release_date":"2026-01-20T00:00:00.000Z"},
                      {"type":6,"release_date":"2026-02-01T00:00:00.000Z"},
                      {"type":1,"release_date":"2026-02-05T00:00:00.000Z"}]}]}}
                """);

        Optional<TmdbMovieDetails> details =
                tmdbService.fetchMovieDetails(102L);

        assertThat(details).isPresent();
        assertThat(details.get().indiaTheatricalReleaseDate()).isNull();
    }

    @Test
    void usesTheLimitedTheatricalDateWhenNoPlainTheatricalDateExists() {

        server.respond("/3/movie/103", """
                {"id":103,"title":"Limited Release","overview":"",
                 "poster_path":null,"backdrop_path":null,
                 "original_language":"en","vote_average":0,
                 "release_dates":{"id":103,"results":[
                   {"iso_3166_1":"IN","release_dates":[
                      {"type":2,"release_date":"2026-04-02T00:00:00.000Z"}]}]}}
                """);

        Optional<TmdbMovieDetails> details =
                tmdbService.fetchMovieDetails(103L);

        assertThat(details).isPresent();
        assertThat(details.get().indiaTheatricalReleaseDate())
                .isEqualTo(LocalDate.of(2026, 4, 2));
    }

    @Test
    void prefersThePlainTheatricalDateOverALimitedTheatricalDate() {

        server.respond("/3/movie/104", """
                {"id":104,"title":"Both","overview":"",
                 "poster_path":null,"backdrop_path":null,
                 "original_language":"en","vote_average":0,
                 "release_dates":{"id":104,"results":[
                   {"iso_3166_1":"IN","release_dates":[
                      {"type":2,"release_date":"2026-01-05T00:00:00.000Z"},
                      {"type":3,"release_date":"2026-06-15T00:00:00.000Z"}]}]}}
                """);

        Optional<TmdbMovieDetails> details =
                tmdbService.fetchMovieDetails(104L);

        assertThat(details).isPresent();
        assertThat(details.get().indiaTheatricalReleaseDate())
                .isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void ignoresTheatricalDatesOfOtherCountries() {

        server.respond("/3/movie/105", """
                {"id":105,"title":"Foreign Date","overview":"",
                 "poster_path":null,"backdrop_path":null,
                 "original_language":"en","vote_average":0,
                 "release_dates":{"id":105,"results":[
                   {"iso_3166_1":"US","release_dates":[
                      {"type":3,"release_date":"2026-01-10T00:00:00.000Z"}]},
                   {"iso_3166_1":"GB","release_dates":[
                      {"type":3,"release_date":"2026-01-11T00:00:00.000Z"}]}]}}
                """);

        Optional<TmdbMovieDetails> details =
                tmdbService.fetchMovieDetails(105L);

        assertThat(details).isPresent();
        assertThat(details.get().indiaTheatricalReleaseDate()).isNull();
    }

    @Test
    void returnsNoDetailsWhenTheApiFails() {

        server.respondWithStatus("/3/movie/106", 500, "{\"error\":\"boom\"}");

        assertThat(tmdbService.fetchMovieDetails(106L)).isEmpty();
    }

    @Test
    void returnsNoDetailsWhenTheApiAnswersWithAnInvalidBody() {

        server.respond("/3/movie/107", "this is not json");

        assertThat(tmdbService.fetchMovieDetails(107L)).isEmpty();
    }

    @Test
    void returnsNoDetailsWhenTheApiTimesOut() {

        server.respondSlowly("/3/movie/108", 1500, """
                {"id":108,"title":"Slow","overview":"","poster_path":null,
                 "backdrop_path":null,"original_language":"en","vote_average":0}
                """);

        TmdbService impatient = service("test-access-token", 2, 250);

        assertThat(impatient.fetchMovieDetails(108L)).isEmpty();
    }

    @Test
    void returnsNoDetailsWhenTheCredentialIsMissing() {

        TmdbService withoutToken = service("", 2, 2000);

        assertThat(withoutToken.fetchMovieDetails(101L)).isEmpty();
    }

    @Test
    void cachesTheDetailsSoTheApiIsNotCalledTwice() {

        server.respond("/3/movie/109", """
                {"id":109,"title":"Cached","overview":"","poster_path":null,
                 "backdrop_path":null,"original_language":"en","vote_average":0,
                 "release_dates":{"id":109,"results":[
                   {"iso_3166_1":"IN","release_dates":[
                      {"type":3,"release_date":"2026-01-10T00:00:00.000Z"}]}]}}
                """);

        tmdbService.fetchMovieDetails(109L);
        tmdbService.fetchMovieDetails(109L);

        long calls = server.requests().stream()
                .filter(request -> request.startsWith("/3/movie/109"))
                .count();

        assertThat(calls).isEqualTo(1);
    }

    @Test
    void cachesTheDiscoveryListsSoTheApiIsNotCalledTwice() {

        server.respond(NOW_PLAYING, """
                {"page":1,"total_pages":1,"results":[
                  {"id":110,"title":"Cached","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"en","vote_average":0}]}
                """);

        tmdbService.fetchDiscoveryCandidates();
        tmdbService.fetchDiscoveryCandidates();

        long calls = server.requests().stream()
                .filter(request -> request.startsWith(NOW_PLAYING))
                .count();

        assertThat(calls).isEqualTo(1);
    }

    // ============================================================
    // Trailer extraction
    // ============================================================

    @Test
    void extractTrailerKeyPrefersTheOfficialTrailer() {

        String json = """
                [
                  {"key":"aaa","site":"YouTube","type":"Trailer","official":false},
                  {"key":"bbb","site":"YouTube","type":"Trailer","official":true},
                  {"key":"ccc","site":"YouTube","type":"Teaser","official":true}
                ]""";

        JsonNode node = new tools.jackson.databind.ObjectMapper().readTree(json);

        assertThat(TmdbService.extractTrailerKey(node)).isEqualTo("bbb");
    }

    @Test
    void extractTrailerKeyFallsBackToAnyTrailerWhenNoOfficialExists() {

        String json = """
                [
                  {"key":"aaa","site":"YouTube","type":"Trailer","official":false},
                  {"key":"bbb","site":"Vimeo","type":"Trailer","official":true}
                ]""";

        JsonNode node = new tools.jackson.databind.ObjectMapper().readTree(json);

        assertThat(TmdbService.extractTrailerKey(node)).isEqualTo("aaa");
    }

    @Test
    void extractTrailerKeyReturnsNullWhenNoTrailerExists() {

        String json = """
                [
                  {"key":"aaa","site":"YouTube","type":"Teaser","official":true},
                  {"key":"bbb","site":"Vimeo","type":"Trailer","official":true}
                ]""";

        JsonNode node = new tools.jackson.databind.ObjectMapper().readTree(json);

        assertThat(TmdbService.extractTrailerKey(node)).isNull();
    }

    @Test
    void extractTrailerKeyReturnsNullForNullInput() {
        assertThat(TmdbService.extractTrailerKey(null)).isNull();
    }

    // ============================================================
    // Cast parsing
    // ============================================================

    @Test
    void fetchCreditsParsesCastMembers() {

        server.respond("/3/movie/200/credits", """
                {"id":200,"cast":[
                  {"id":1,"name":"Actor One","character":"Hero","profile_path":"/p1.jpg"},
                  {"id":2,"name":"Actor Two","character":"Villain","profile_path":null}
                ],"crew":[]}""");

        List<TmdbCastMember> cast = tmdbService.fetchCredits(200L);

        assertThat(cast).hasSize(2);
        assertThat(cast.get(0).name()).isEqualTo("Actor One");
        assertThat(cast.get(0).character()).isEqualTo("Hero");
        assertThat(cast.get(0).profilePath()).isEqualTo("/p1.jpg");
        assertThat(cast.get(1).name()).isEqualTo("Actor Two");
        assertThat(cast.get(1).profilePath()).isEmpty();
    }

    @Test
    void fetchCreditsReturnsEmptyWhenApiFails() {

        server.respondWithStatus("/3/movie/201/credits", 500, "{\"error\":\"boom\"}");

        assertThat(tmdbService.fetchCredits(201L)).isEmpty();
    }

    @Test
    void fetchCreditsReturnsEmptyWhenNoCastExists() {

        server.respond("/3/movie/202/credits", """
                {"id":202,"cast":[],"crew":[]}""");

        assertThat(tmdbService.fetchCredits(202L)).isEmpty();
    }

    @Test
    void fetchTrailerKeyReturnsNullWhenNoTrailerExists() {

        server.respond("/3/movie/203/videos", """
                {"id":203,"results":[
                  {"key":"aaa","site":"YouTube","type":"Teaser","official":true}
                ]}""");

        assertThat(tmdbService.fetchTrailerKey(203L)).isNull();
    }

    @Test
    void fetchTrailerKeyReturnsNullWhenApiFails() {

        server.respondWithStatus("/3/movie/204/videos", 500, "{\"error\":\"boom\"}");

        assertThat(tmdbService.fetchTrailerKey(204L)).isNull();
    }

    @Test
    void fetchTrailerKeyCachesResult() {

        server.respond("/3/movie/205/videos", """
                {"id":205,"results":[
                  {"key":"trailer-key","site":"YouTube","type":"Trailer","official":true}
                ]}""");

        tmdbService.fetchTrailerKey(205L);
        tmdbService.fetchTrailerKey(205L);

        long calls = server.requests().stream()
                .filter(request -> request.startsWith("/3/movie/205/videos"))
                .count();

        assertThat(calls).isEqualTo(1);
    }

    @Test
    void exposesTheExistingSearchContract() {

        server.respond("/3/movie/now_playing", """
                {"page":1,"total_pages":1,"results":[
                  {"id":111,"title":"Searchable","overview":"","poster_path":null,
                   "backdrop_path":null,"original_language":"en","vote_average":0}]}
                """);

        assertThat(tmdbService.hasCredentials()).isTrue();
        assertThat(tmdbService.getPosterUrl(null)).isNull();
        assertThat(tmdbService.getBackdropUrl(null)).isNull();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private TmdbService service(String token, int maxPages, int readTimeoutMs) {
        return new TmdbService(
                null,
                token,
                server.baseUrl(),
                "IN",
                "en-IN",
                1000,
                readTimeoutMs,
                maxPages,
                30,
                360);
    }

    @Test
    void keepsTheCandidateListEmptyWhenNoListIsAvailable() {

        TmdbService.DiscoveryResult result =
                tmdbService.fetchDiscoveryCandidates();

        List<TmdbMovieCandidate> candidates = result.candidates();

        assertThat(candidates).isEmpty();
    }
}
