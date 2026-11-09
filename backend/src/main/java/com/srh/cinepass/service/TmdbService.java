package com.srh.cinepass.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class TmdbService {

    @Value("${tmdb.access-token}")
    private String accessToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private JsonNode sendRequest(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "TMDB API returned status: " + response.statusCode());
            }

            return objectMapper.readTree(response.body());

        } catch (Exception e) {
            throw new RuntimeException("TMDB request failed: " + e.getMessage(), e);
        }
    }

    // Search all matching movies
    public JsonNode searchMovies(String title) {
        String query = URLEncoder.encode(title, StandardCharsets.UTF_8);

        String url = "https://api.themoviedb.org/3/search/movie"
                + "?query=" + query
                + "&include_adult=false"
                + "&language=en-US"
                + "&page=1";

        return sendRequest(url).path("results");
    }

    // Fetch full movie details using verified TMDB ID
    public JsonNode getMovieDetails(Long tmdbId) {
        String url = "https://api.themoviedb.org/3/movie/" + tmdbId
                + "?language=en-US";

        return sendRequest(url);
    }

    public String getPosterUrl(JsonNode movie) {
        String path = movie.path("poster_path").asText("");

        return path.isBlank()
                ? null
                : "https://image.tmdb.org/t/p/w500" + path;
    }

    public String getBackdropUrl(JsonNode movie) {
        String path = movie.path("backdrop_path").asText("");

        return path.isBlank()
                ? null
                : "https://image.tmdb.org/t/p/w1280" + path;
    }
}