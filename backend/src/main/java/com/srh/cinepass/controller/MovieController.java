package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "http://localhost:5173")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // GET all movies
    @GetMapping
    public List<Movie> getAllMovies() {
        return movieService.getAllMovies();
    }

    // GET movie by ID
    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {

        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ADD movie
    @PostMapping
    public ResponseEntity<Movie> addMovie(@RequestBody Movie movie) {

        Movie savedMovie = movieService.addMovie(movie);

        return ResponseEntity.ok(savedMovie);
    }

    // UPDATE movie
    @PutMapping("/{id}")
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

    // DELETE movie
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {

        try {
            movieService.deleteMovie(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }
}