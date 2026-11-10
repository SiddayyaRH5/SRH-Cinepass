package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class TmdbService {

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate;

    @Value("${tmdb.access.token:}")
    private String tmdbAccessToken;

    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/search/movie?query=";
    private static final String IMAGE_BASE_POSTER = "https://image.tmdb.org/t/p/w500";
    private static final String IMAGE_BASE_BACKDROP = "https://image.tmdb.org/t/p/w1280";

    public TmdbService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
        this.restTemplate = new RestTemplate();
    }

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

    @SuppressWarnings("unchecked")
    public boolean fetchAndUpdateMovieMetadata(Movie movie) {
        try {
            if (tmdbAccessToken == null || tmdbAccessToken.isBlank()) {
                return false;
            }

            String url = TMDB_SEARCH_URL + java.net.URLEncoder.encode(movie.getTitle(), java.nio.charset.StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tmdbAccessToken);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map bodyMap = (Map) response.getBody();
                List results = (List) bodyMap.get("results");
                if (results != null && !results.isEmpty()) {
                    Map bestMatch = (Map) results.get(0);

                    String posterPath = (String) bestMatch.get("poster_path");
                    String backdropPath = (String) bestMatch.get("backdrop_path");
                    Object voteAverage = bestMatch.get("vote_average");
                    String overview = (String) bestMatch.get("overview");
                    String releaseDateStr = (String) bestMatch.get("release_date");

                    if (posterPath != null && !posterPath.isBlank()) {
                        movie.setPosterUrl(IMAGE_BASE_POSTER + posterPath);
                    }
                    if (backdropPath != null && !backdropPath.isBlank()) {
                        movie.setBackdropUrl(IMAGE_BASE_BACKDROP + backdropPath);
                    }
                    if (overview != null && (movie.getDescription() == null || movie.getDescription().isBlank())) {
                        movie.setDescription(overview);
                    }
                    if (voteAverage instanceof Number && (movie.getRating() == null || movie.getRating() == 0.0)) {
                        movie.setRating(((Number) voteAverage).doubleValue());
                    }
                    if (releaseDateStr != null && !releaseDateStr.isBlank() && movie.getReleaseDate() == null) {
                        try {
                            movie.setReleaseDate(LocalDate.parse(releaseDateStr));
                        } catch (Exception ignored) {}
                    }

                    movieRepository.save(movie);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch TMDB metadata for movie: " + movie.getTitle() + " -> " + e.getMessage());
        }
        return false;
    }
}