package com.srh.cinepass.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.srh.cinepass.dto.TmdbCastMember;
import com.srh.cinepass.dto.TmdbMovieCandidate;
import com.srh.cinepass.dto.TmdbMovieDetails;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.repository.MovieRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * The single TMDB client of the application.
 *
 * <p>TMDB is the only external movie metadata and release discovery provider.
 * The access token is read from a backend-only environment variable and is
 * never written to a log line, a response body or the frontend bundle.</p>
 *
 * <p>Endpoints used (see the official TMDB API reference):</p>
 * <ul>
 *   <li>{@code GET /3/movie/now_playing?region=IN} - movies in Indian theatres.</li>
 *   <li>{@code GET /3/movie/upcoming?region=IN} - movies releasing soon in India.</li>
 *   <li>{@code GET /3/movie/{id}?append_to_response=release_dates} - full metadata
 *       plus the per-country release dates.</li>
 *   <li>{@code GET /3/search/movie?query=} - the existing admin search.</li>
 * </ul>
 *
 * <p>The regional lists are only candidates. A movie is promoted into the
 * catalogue exclusively through its confirmed <em>Indian theatrical</em>
 * release date, which is read from the {@code release_dates} block and only
 * accepts the theatrical release types 2 (limited) and 3 (theatrical).
 * Premiere (1), Digital (4), Physical (5) and TV (6) dates are never used,
 * and a worldwide date is never treated as the Indian theatrical date.</p>
 *
 * <p>Every call is bounded by connect and read timeouts and the results are
 * cached for a short period, so a daily job never hammers the API.</p>
 */
@Service
public class TmdbService {

    private static final Logger log =
            LoggerFactory.getLogger(TmdbService.class);

    private static final String IMAGE_BASE_POSTER = "https://image.tmdb.org/t/p/w500";
    private static final String IMAGE_BASE_BACKDROP = "https://image.tmdb.org/t/p/w1280";

    /**
     * TMDB release types that represent a commercial theatrical release.
     * Everything else is ignored for Indian theatrical decisions.
     */
    public static final Set<Integer> THEATRICAL_RELEASE_TYPES = Set.of(2, 3);

    /** TMDB release type for a theatrical (limited) release. */
    private static final int TYPE_THEATRICAL_LIMITED = 2;

    /** TMDB release type for a plain theatrical release. */
    private static final int TYPE_THEATRICAL = 3;

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate;

    private final String tmdbAccessToken;
    private final String baseUrl;
    private final String region;
    private final String language;
    private final int discoveryMaxPages;

    private final Cache<String, DiscoveryResult> discoveryCache;
    private final Cache<Long, TmdbMovieDetails> detailsCache;
    private final Cache<Long, List<TmdbCastMember>> creditsCache;
    private final Cache<Long, String> videosCache;

    public TmdbService(
            MovieRepository movieRepository,
            @Value("${tmdb.access.token:}") String tmdbAccessToken,
            @Value("${app.tmdb.base-url:https://api.themoviedb.org/3}") String baseUrl,
            @Value("${app.tmdb.region:IN}") String region,
            @Value("${app.tmdb.language:en-IN}") String language,
            @Value("${app.tmdb.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.tmdb.read-timeout-ms:10000}") int readTimeoutMs,
            @Value("${app.tmdb.discovery-max-pages:3}") int discoveryMaxPages,
            @Value("${app.tmdb.cache.discovery-ttl-minutes:30}") long discoveryTtlMinutes,
            @Value("${app.tmdb.cache.details-ttl-minutes:360}") long detailsTtlMinutes) {

        this.movieRepository = movieRepository;
        this.tmdbAccessToken = tmdbAccessToken;
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.region = region;
        this.language = language;
        this.discoveryMaxPages = Math.max(1, discoveryMaxPages);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(500, connectTimeoutMs)));
        factory.setReadTimeout(Duration.ofMillis(Math.max(500, readTimeoutMs)));
        this.restTemplate = new RestTemplate(factory);

        this.discoveryCache = Caffeine.newBuilder()
                .maximumSize(20)
                .expireAfterWrite(Duration.ofMinutes(Math.max(1, discoveryTtlMinutes)))
                .build();

        this.detailsCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofMinutes(Math.max(1, detailsTtlMinutes)))
                .build();

        this.creditsCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofMinutes(Math.max(1, detailsTtlMinutes)))
                .build();

        this.videosCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofMinutes(Math.max(1, detailsTtlMinutes)))
                .build();
    }

    // ============================================================
    // Existing API (unchanged public contract)
    // ============================================================

    public int syncAllMoviePosters() {
        List<Movie> movies = movieRepository.findAll();
        int updatedCount = 0;

        for (Movie movie : movies) {
            if (movie.getPosterUrl() == null || movie.getPosterUrl().isBlank() ||
                movie.getBackdropUrl() == null || movie.getBackdropUrl().isBlank()) {
                boolean updated = fetchAndUpdateMovieMetadata(movie);
                if (updated) {
                    updatedCount++;
                }
            }
        }
        return updatedCount;
    }

    public boolean fetchAndUpdateMovieMetadata(Movie movie) {
    if (!hasCredentials() || movie == null || movie.getTmdbId() == null) {
        return false;
    }

    try {
        JsonNode details = getMovieDetails(movie.getTmdbId());

        if (details == null || details.isNull()) {
            return false;
        }

        String posterUrl = getPosterUrl(details);
        if (posterUrl == null || posterUrl.isBlank()) {
            return false;
        }

        movie.setPosterUrl(posterUrl);

        String backdropUrl = getBackdropUrl(details);
        if (backdropUrl != null && !backdropUrl.isBlank()) {
            movie.setBackdropUrl(backdropUrl);
        }

        movieRepository.save(movie);
        return true;
    } catch (Exception e) {
        log.warn("Poster update failed for movie {}: {}", movie.getId(), e.getMessage());
        return false;
    }
}

    public JsonNode searchMovies(String query) {
        try {
            String url = baseUrl + "/search/movie?query="
                    + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            return getJson(url);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode getMovieDetails(Long tmdbId) {
        try {
            return getJson(baseUrl + "/movie/" + tmdbId);
        } catch (Exception e) {
            return null;
        }
    }

    public String getPosterUrl(JsonNode node) {
        if (node == null) {
            return null;
        }

        String path = text(node, "poster_path");

        return path.isEmpty() ? null : IMAGE_BASE_POSTER + path;
    }

    public String getBackdropUrl(JsonNode node) {
        if (node == null) {
            return null;
        }

        String path = text(node, "backdrop_path");

        return path.isEmpty() ? null : IMAGE_BASE_BACKDROP + path;
    }

    // ============================================================
    // Discovery (used by the automatic catalogue sync)
    // ============================================================

    /**
     * Result of one discovery sweep.
     *
     * @param candidates      the movies found in the regional lists
     * @param pagesProcessed  how many TMDB pages were actually read
     * @param errors          short, credential-free failure descriptions
     */
    public record DiscoveryResult(
            List<TmdbMovieCandidate> candidates,
            int pagesProcessed,
            List<String> errors) {

        public boolean isEmpty() {
            return candidates == null || candidates.isEmpty();
        }
    }

    /**
     * Reads the Indian "now playing" and "upcoming" lists.
     *
     * <p>Both lists are regional, so they only contain movies that TMDB
     * associates with India. A movie is nevertheless only a candidate here:
     * its Indian theatrical release date is confirmed separately.</p>
     */
    public DiscoveryResult fetchDiscoveryCandidates() {

        List<String> errors = new ArrayList<>();

        if (!hasCredentials()) {
            log.warn("TMDB discovery skipped: no access token is configured");
            errors.add("tmdb access token is not configured");
            return new DiscoveryResult(List.of(), 0, errors);
        }

        String cacheKey = region + "|" + language + "|" + discoveryMaxPages;

        DiscoveryResult cached = discoveryCache.getIfPresent(cacheKey);

        if (cached != null) {
            log.debug("TMDB discovery served from cache ({} candidates)", cached.candidates().size());
            return cached;
        }

        Map<Long, TmdbMovieCandidate> merged = new LinkedHashMap<>();
        int pagesProcessed = 0;

        for (String listUrl : List.of(
                baseUrl + "/movie/now_playing",
                baseUrl + "/movie/upcoming")) {

            for (int page = 1; page <= discoveryMaxPages; page++) {

                String url = listUrl
                        + "?region=" + region
                        + "&language=" + language
                        + "&page=" + page;

                JsonNode body = getJson(url);

                if (body == null || body.isNull()) {
                    errors.add("unreadable page for " + listUrl + " page " + page);
                    break;
                }

                pagesProcessed++;

                JsonNode results = body.path("results");

                if (!results.isArray() || results.isEmpty()) {
                    break;
                }

                for (JsonNode node : results) {
                    TmdbMovieCandidate candidate = parseCandidate(node);
                    if (candidate != null) {
                        merged.putIfAbsent(candidate.tmdbId(), candidate);
                    }
                }

                int totalPages = body.path("total_pages").asInt(1);

                if (page >= totalPages) {
                    break;
                }
            }
        }

        List<TmdbMovieCandidate> candidates = List.copyOf(merged.values());

        DiscoveryResult result = new DiscoveryResult(candidates, pagesProcessed, errors);

        if (!candidates.isEmpty()) {
            discoveryCache.put(cacheKey, result);
        }

        log.info(
                "TMDB discovery finished: {} candidate(s) from {} page(s) for region {}",
                candidates.size(),
                pagesProcessed,
                region);

        return result;
    }

    // ============================================================
    // Movie details including the Indian theatrical release date
    // ============================================================

    /**
     * Reads the full metadata of one movie together with its release dates.
     *
     * @return the details, or {@link Optional#empty()} when TMDB has no usable
     *         record for this identifier
     */
    public Optional<TmdbMovieDetails> fetchMovieDetails(Long tmdbId) {

        if (tmdbId == null || tmdbId <= 0 || !hasCredentials()) {
            return Optional.empty();
        }

        TmdbMovieDetails cached = detailsCache.getIfPresent(tmdbId);

        if (cached != null) {
            return Optional.of(cached);
        }

        String url = baseUrl + "/movie/" + tmdbId
                + "?append_to_response=release_dates"
                + "&language=" + language;

        JsonNode node = getJson(url);

        if (node == null || node.isNull()) {
            log.warn("TMDB details unavailable for movie {}", tmdbId);
            return Optional.empty();
        }

        long returnedId = node.path("id").asLong(0);

        if (returnedId != tmdbId) {
            throw new IllegalStateException(
                    "TMDB ID verification failed for movie " + tmdbId);
        }

        String title = text(node, "title");

        if (title.isBlank()) {
            return Optional.empty();
        }

        Double rating = node.path("vote_average").isNumber()
                ? node.path("vote_average").asDouble()
                : null;

        Integer runtime = node.path("runtime").isNumber()
                && node.path("runtime").asInt() > 0
                ? node.path("runtime").asInt()
                : null;

        TmdbMovieDetails details = new TmdbMovieDetails(
                tmdbId,
                title,
                text(node, "overview"),
                getPosterUrl(node),
                getBackdropUrl(node),
                text(node, "original_language"),
                parseGenres(node.path("genres")),
                rating,
                runtime,
                extractIndiaTheatricalReleaseDate(node.path("release_dates"))
                        .orElse(null),
                null,
                List.of());

        detailsCache.put(tmdbId, details);

        return Optional.of(details);
    }

    /**
     * Extracts the Indian theatrical release date from a TMDB
     * {@code release_dates} block.
     *
     * <p>Rules, applied strictly:</p>
     * <ol>
     *   <li>Only the {@code IN} country block is considered.</li>
     *   <li>Only the theatrical release types 2 and 3 are considered. Digital,
     *       physical, TV and premiere dates are ignored.</li>
     *   <li>A type 3 date wins. If only type 2 dates exist, the earliest type 2
     *       date is used.</li>
     * </ol>
     *
     * @return the Indian theatrical release date, or empty when TMDB has none
     */
    public static Optional<LocalDate> extractIndiaTheatricalReleaseDate(JsonNode releaseDates) {

        if (releaseDates == null || releaseDates.isNull()) {
            return Optional.empty();
        }

        JsonNode countries = releaseDates.path("results");

        if (!countries.isArray()) {
            return Optional.empty();
        }

        LocalDate theatrical = null;
        LocalDate limited = null;

        for (JsonNode country : countries) {

            if (!"IN".equalsIgnoreCase(country.path("iso_3166_1").asText(""))) {
                continue;
            }

            JsonNode entries = country.path("release_dates");

            if (!entries.isArray()) {
                continue;
            }

            for (JsonNode entry : entries) {

                int type = entry.path("type").asInt(0);

                if (!THEATRICAL_RELEASE_TYPES.contains(type)) {
                    continue;
                }

                LocalDate date = parseReleaseDate(entry.path("release_date").asText(""));

                if (date == null) {
                    continue;
                }

                if (type == TYPE_THEATRICAL) {
                    if (theatrical == null || date.isBefore(theatrical)) {
                        theatrical = date;
                    }
                } else if (type == TYPE_THEATRICAL_LIMITED) {
                    if (limited == null || date.isBefore(limited)) {
                        limited = date;
                    }
                }
            }
        }

        return Optional.ofNullable(theatrical != null ? theatrical : limited);
    }

    // ============================================================
    // Credits and videos (cast and trailer)
    // ============================================================

    /**
     * Reads the principal cast of a movie. Returns an empty list when TMDB
     * has no cast data or the request fails.
     */
    public List<TmdbCastMember> fetchCredits(Long tmdbId) {

        if (tmdbId == null || tmdbId <= 0 || !hasCredentials()) {
            return List.of();
        }

        List<TmdbCastMember> cached = creditsCache.getIfPresent(tmdbId);

        if (cached != null) {
            return cached;
        }

        JsonNode node = getJson(baseUrl + "/movie/" + tmdbId + "/credits");

        if (node == null || node.isNull()) {
            log.warn("TMDB credits unavailable for movie {}", tmdbId);
            return List.of();
        }

        List<TmdbCastMember> cast = parseCast(node.path("cast"));

        creditsCache.put(tmdbId, cast);

        return cast;
    }

    /**
     * Reads the YouTube trailer key for a movie. Returns {@code null} when
     * no suitable trailer is available or the request fails.
     */
    public String fetchTrailerKey(Long tmdbId) {

        if (tmdbId == null || tmdbId <= 0 || !hasCredentials()) {
            return null;
        }

        String cached = videosCache.getIfPresent(tmdbId);

        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        JsonNode node = getJson(baseUrl + "/movie/" + tmdbId + "/videos");

        if (node == null || node.isNull()) {
            log.warn("TMDB videos unavailable for movie {}", tmdbId);
            return null;
        }

        String key = extractTrailerKey(node.path("results"));

        videosCache.put(tmdbId, key == null ? "" : key);

        return key;
    }

    /**
     * Extracts the best YouTube trailer key from a TMDB videos result array.
     *
     * <p>Preference order:</p>
     * <ol>
     *   <li>Official YouTube trailer.</li>
     *   <li>Any YouTube trailer.</li>
     * </ol>
     *
     * @return the video key, or {@code null} when none is available
     */
    public static String extractTrailerKey(JsonNode results) {

        if (results == null || !results.isArray()) {
            return null;
        }

        String officialTrailer = null;
        String anyTrailer = null;

        for (JsonNode video : results) {

            String site = text(video, "site");
            String type = text(video, "type");
            String key = text(video, "key");

            if (!"YouTube".equalsIgnoreCase(site) || key.isEmpty()) {
                continue;
            }

            if (!"Trailer".equalsIgnoreCase(type)) {
                continue;
            }

            if (anyTrailer == null) {
                anyTrailer = key;
            }

            if (video.path("official").asBoolean(false) && officialTrailer == null) {
                officialTrailer = key;
            }
        }

        return officialTrailer != null ? officialTrailer : anyTrailer;
    }

    /**
     * @return {@code true} when a TMDB access token is configured
     */
    public boolean hasCredentials() {
        return tmdbAccessToken != null && !tmdbAccessToken.isBlank();
    }

    // ============================================================
    // Internals
    // ============================================================

    private TmdbMovieCandidate parseCandidate(JsonNode node) {

        long id = node.path("id").asLong(0);

        if (id <= 0) {
            return null;
        }

        String title = text(node, "title");

        if (title.isBlank()) {
            return null;
        }

        return new TmdbMovieCandidate(
                id,
                title,
                text(node, "overview"),
                text(node, "poster_path"),
                text(node, "backdrop_path"),
                text(node, "original_language"),
                node.path("vote_average").asDouble(0.0));
    }

    /**
     * Reads a string field. A missing field, a JSON {@code null} and the
     * literal string {@code "null"} (which TMDB sends for absent images) all
     * collapse to an empty string.
     */
    private static String text(JsonNode node, String field) {

        if (node == null) {
            return "";
        }

        JsonNode value = node.path(field);

        if (value == null || value.isNull()) {
            return "";
        }

        String result = value.asText("");

        if (result == null || result.isBlank() || "null".equals(result)) {
            return "";
        }

        return result;
    }

    private static String parseGenres(JsonNode genres) {

        if (genres == null || !genres.isArray() || genres.isEmpty()) {
            return "";
        }

        return StreamSupport.stream(genres.spliterator(), false)
                .map(genre -> genre.path("name").asText(""))
                .filter(name -> !name.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    /**
     * Parses up to 15 principal cast members from a TMDB credits cast array.
     */
    private List<TmdbCastMember> parseCast(JsonNode castArray) {

        List<TmdbCastMember> result = new ArrayList<>();

        if (castArray == null || !castArray.isArray()) {
            return result;
        }

        int count = 0;

        for (JsonNode member : castArray) {

            if (count >= 15) {
                break;
            }

            String name = text(member, "name");

            if (name.isEmpty()) {
                continue;
            }

            String character = text(member, "character");
            String profilePath = text(member, "profile_path");

            result.add(new TmdbCastMember(name, character, profilePath));

            count++;
        }

        return result;
    }

    private static LocalDate parseReleaseDate(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();

        // TMDB returns an ISO timestamp such as 2024-01-05T00:00:00.000Z.
        if (value.length() > 10) {
            value = value.substring(0, 10);
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * Performs a GET request with the bearer token attached. The token is
     * never logged and never leaves the backend.
     */
    private JsonNode getJson(String url) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tmdbAccessToken);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            return response.getBody();

        } catch (Exception ex) {
            // The message may contain the URL but never the token.
            log.warn("TMDB request failed: {}", ex.getMessage());
            return null;
        }
    }
}
