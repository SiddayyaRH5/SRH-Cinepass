package com.srh.cinepass.service;

import tools.jackson.databind.JsonNode;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@Service
public class TmdbService {

    private final MovieRepository movieRepository;
    private final RestTemplate restTemplate;

    @Value("${tmdb.access.token:}")
    private String tmdbAccessToken;

    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/search/movie?query=";
    private static final String TMDB_DETAILS_URL = "https://api.themoviedb.org/3/movie/";
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

    public boolean fetchAndUpdateMovieMetadata(Movie movie) {
    if (tmdbAccessToken == null || tmdbAccessToken.isBlank()
            || movie.getTmdbId() == null) {
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
        System.err.println("Poster update failed for " + movie.getTitle()
            + ": " + e.getMessage());
        return false;
    }
}

    public JsonNode searchMovies(String query) {
        try {
            String url = TMDB_SEARCH_URL + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tmdbAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode getMovieDetails(Long tmdbId) {
        try {
            String url = TMDB_DETAILS_URL + tmdbId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tmdbAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public String getPosterUrl(JsonNode node) {
        if (node != null && node.has("poster_path")) {
            String path = node.get("poster_path").asText();
            if (path != null && !path.isBlank() && !path.equals("null")) {
                return IMAGE_BASE_POSTER + path;
            }
        }
        return null;
    }

    public String getBackdropUrl(JsonNode node) {
        if (node != null && node.has("backdrop_path")) {
            String path = node.get("backdrop_path").asText();
            if (path != null && !path.isBlank() && !path.equals("null")) {
                return IMAGE_BASE_BACKDROP + path;
            }
        }
        return null;
    }
}