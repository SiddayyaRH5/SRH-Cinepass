package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.repository.MovieRepository;
import org.springframework.stereotype.Service;
<<<<<<< HEAD
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
=======
>>>>>>> origin/main

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // Get all movies
<<<<<<< HEAD
    @Cacheable(cacheNames = "movies")
=======
>>>>>>> origin/main
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    // Get movie by ID
    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    // Add movie
<<<<<<< HEAD
    @CacheEvict(cacheNames = "movies", allEntries = true)
=======
>>>>>>> origin/main
    public Movie addMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    // Update movie
<<<<<<< HEAD
    @CacheEvict(cacheNames = "movies", allEntries = true)
=======
>>>>>>> origin/main
    public Movie updateMovie(Long id, Movie movieDetails) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Movie not found with id: " + id));

        movie.setTitle(movieDetails.getTitle());
        movie.setDescription(movieDetails.getDescription());
        movie.setPosterUrl(movieDetails.getPosterUrl());
        movie.setBackdropUrl(movieDetails.getBackdropUrl());
        movie.setReleaseDate(movieDetails.getReleaseDate());
        movie.setRating(movieDetails.getRating());
        movie.setDuration(movieDetails.getDuration());
        movie.setLanguage(movieDetails.getLanguage());
        movie.setGenre(movieDetails.getGenre());

        return movieRepository.save(movie);
    }

    // Delete movie
<<<<<<< HEAD
    @CacheEvict(cacheNames = "movies", allEntries = true)
=======
>>>>>>> origin/main
    public void deleteMovie(Long id) {

        if (!movieRepository.existsById(id)) {
            throw new RuntimeException(
                    "Movie not found with id: " + id);
        }

        movieRepository.deleteById(id);
    }
}