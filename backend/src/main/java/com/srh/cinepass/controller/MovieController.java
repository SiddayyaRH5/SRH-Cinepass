package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.service.MovieService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // GET all movies (cached)
    @GetMapping
    @Cacheable(value = "movies", unless = "#result == null || #result.isEmpty()")
    public List<Movie> getAllMovies() {
        return movieService.getAllMovies();
    }

    // GET movie by ID
    // Archived movies return 404 because the service layer filters them out
    // from the cache. The controller maps Optional.empty() to 404.
    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .filter(m -> m.getStatus() != MovieStatus.ARCHIVED)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ADD movie (evict cache)
    @PostMapping
    @CacheEvict(value = "movies", allEntries = true)
    public ResponseEntity<Movie> addMovie(@RequestBody Movie movie) {

        Movie savedMovie = movieService.addMovie(movie);

        return ResponseEntity.ok(savedMovie);
    }

    // UPDATE movie (evict cache)
    @PutMapping("/{id}")
    @CacheEvict(value = {"movies", "movie-details"}, allEntries = true)
    public ResponseEntity<Movie> updateMovie(
            @PathVariable Long id,
            @RequestBody Movie movieDetails) {

        try {
            Movie updatedMovie =
                    movieService.updateMovie(id, movieDetails);

            return ResponseEntity.ok(updatedMovie);

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    // DELETE movie (evict cache)
    @DeleteMapping("/{id}")
    @CacheEvict(value = {"movies", "movie-details"}, allEntries = true)
    public ResponseEntity<?> deleteMovie(@PathVariable Long id) {

        try {
            movieService.deleteMovie(id);

            return ResponseEntity.noContent().build();

        } catch (IllegalStateException e) {

            // The movie has history and must not be deleted.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }
}
