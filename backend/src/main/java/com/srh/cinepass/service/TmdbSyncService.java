
package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.stream.StreamSupport;

@Service
public class TmdbSyncService {

    private final TmdbService tmdbService;
    private final MovieRepository movieRepository;

    public TmdbSyncService(
            TmdbService tmdbService,
            MovieRepository movieRepository) {
        this.tmdbService = tmdbService;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public Movie syncMovie(Long movieId, Long tmdbId) {

        // Find existing movie in our database
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));

        // Fetch verified movie details from TMDB
        JsonNode details = tmdbService.getMovieDetails(tmdbId);

        // Verify returned TMDB ID
        if (details.path("id").asLong(-1) != tmdbId) {
            throw new RuntimeException("TMDB ID verification failed");
        }

        movie.setTmdbId(tmdbId);

        String overview = details.path("overview").asText("");
        if (!overview.isBlank()) {
            movie.setDescription(overview);
        }

        String poster = tmdbService.getPosterUrl(details);
        if (poster != null) {
            movie.setPosterUrl(poster);
        }

        String backdrop = tmdbService.getBackdropUrl(details);
        if (backdrop != null) {
            movie.setBackdropUrl(backdrop);
        }

        String releaseDate = details.path("release_date").asText("");
        if (!releaseDate.isBlank()) {
            movie.setReleaseDate(LocalDate.parse(releaseDate));
        }

        if (details.path("vote_average").isNumber()) {
            movie.setRating(details.path("vote_average").asDouble());
        }

        if (details.path("runtime").isNumber()
                && details.path("runtime").asInt() > 0) {
            movie.setDuration(details.path("runtime").asInt());
        }

        String language = details.path("original_language").asText("");
        if (!language.isBlank()) {
            movie.setLanguage(language);
        }

        JsonNode genres = details.path("genres");

        if (genres.isArray() && !genres.isEmpty()) {
            String genreNames = StreamSupport.stream(
                    genres.spliterator(), false)
                    .map(genre -> genre.path("name").asText(""))
                    .filter(name -> !name.isBlank())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            if (!genreNames.isBlank()) {
                movie.setGenre(genreNames);
            }
        }

        return movieRepository.save(movie);
    }
}
