package com.srh.cinepass.service;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.MovieSource;
import com.srh.cinepass.entity.MovieStatus;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.repository.ShowRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final CacheManager cacheManager;

    public MovieService(
            MovieRepository movieRepository,
            ShowRepository showRepository,
            CacheManager cacheManager) {

        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * The customer-facing catalogue.
     *
     * <p>Retired movies are not listed here. They keep their row, their
     * shows and their history and remain reachable through the direct movie
     * detail endpoint.</p>
     */
    @Transactional(readOnly = true)
    public List<Movie> getAllMovies() {
        return movieRepository.findActiveMovies(MovieStatus.ACTIVE);
    }

    /**
     * Every movie, including the retired ones. Used by administrative
     * tooling only.
     */
    @Transactional(readOnly = true)
    public List<Movie> getAllMoviesIncludingArchived() {
        return movieRepository.findAll();
    }

    // Get movie by ID
    @Transactional(readOnly = true)
    public Optional<Movie> getMovieById(Long id) {
        var cache = cacheManager.getCache("movie-details");
        var cached = cache.get(id, Movie.class);
        if (cached != null) {
            return Optional.of(cached);
        }

        var movie = movieRepository.findById(id);
        // Only cache non-archived movies. Archived movies should not be
        // served from the cache, and should return 404 from the controller.
        if (movie.isPresent() && movie.get().getStatus() != MovieStatus.ARCHIVED) {
            cache.put(id, movie.get());
        }
        return movie;
    }

    /**
     * Add movie.
     *
     * <p>A movie created through the API is a manually managed movie. It is
     * never refreshed by the automatic TMDB synchronisation, and it always
     * starts in the ACTIVE lifecycle state.</p>
     */
    @Transactional
    public Movie addMovie(Movie movie) {

        if (movie.getSource() == null) {
            movie.setSource(MovieSource.MANUAL);
        }

        if (movie.getStatus() == null) {
            movie.setStatus(MovieStatus.ACTIVE);
        }

        return movieRepository.save(movie);
    }

    // Update movie
    @Transactional
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

    /**
     * Delete movie.
     *
     * <p>A movie that already has shows, bookings, tickets or payments is
     * never physically deleted. Retiring it through
     * {@code POST /api/admin/movies/{id}/archive} keeps the whole history
     * intact, which is the only supported way to remove a movie from the
     * catalogue.</p>
     */
    @Transactional
    public void deleteMovie(Long id) {

        if (!movieRepository.existsById(id)) {
            throw new RuntimeException(
                    "Movie not found with id: " + id);
        }

        if (!showRepository.findByMovieId(id).isEmpty()) {
            throw new IllegalStateException(
                    "This movie has shows and booking history and cannot be deleted. "
                            + "Archive it instead so the history is preserved.");
        }

        movieRepository.deleteById(id);
    }
}
