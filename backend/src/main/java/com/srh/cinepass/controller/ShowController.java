package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.ShowService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@CrossOrigin(origins = "http://localhost:5173")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    // ============================================================
    // GET ALL SHOWS
    // PUBLIC
    // ============================================================

    @GetMapping
    public List<Show> getAllShows() {
        return showService.getAllShows();
    }

    // ============================================================
    // GET SHOW BY ID
    // PUBLIC
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<Show> getShowById(
            @PathVariable Long id) {

        try {
            return ResponseEntity.ok(
                    showService.getShowById(id)
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // ============================================================
    // ADD SHOW
    // ADMIN + APPROVED THEATRE OWNER
    // ============================================================

    @PostMapping
    public ResponseEntity<?> addShow(
            @RequestBody Show show,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            boolean isAdmin =
                    "ADMIN".equalsIgnoreCase(
                            user.getRole()
                    );

            Show savedShow =
                    showService.addShow(
                            show,
                            user.getId(),
                            isAdmin
                    );

            return ResponseEntity.ok(savedShow);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // UPDATE SHOW
    // ADMIN + APPROVED THEATRE OWNER
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateShow(
            @PathVariable Long id,
            @RequestBody Show showDetails,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            boolean isAdmin =
                    "ADMIN".equalsIgnoreCase(
                            user.getRole()
                    );

            if (showDetails.getMovie() == null ||
                    showDetails.getMovie().getId() == null) {

                throw new RuntimeException(
                        "Movie is required"
                );
            }

            Long movieId =
                    showDetails
                            .getMovie()
                            .getId();

            Long theatreId = null;

            if (showDetails.getTheatre() != null &&
                    showDetails.getTheatre().getId() != null) {

                theatreId =
                        showDetails
                                .getTheatre()
                                .getId();
            }

            Show updatedShow =
                    showService.updateShow(
                            id,
                            movieId,
                            theatreId,
                            showDetails,
                            user.getId(),
                            isAdmin
                    );

            return ResponseEntity.ok(updatedShow);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // DELETE SHOW
    // ADMIN + APPROVED THEATRE OWNER
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShow(
            @PathVariable Long id,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            boolean isAdmin =
                    "ADMIN".equalsIgnoreCase(
                            user.getRole()
                    );

            showService.deleteShow(
                    id,
                    user.getId(),
                    isAdmin
            );

            return ResponseEntity
                    .noContent()
                    .build();

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================================================
    // GET SHOWS BY MOVIE
    // PUBLIC
    // ============================================================

    @GetMapping("/movie/{movieId}")
    public List<Show> getShowsByMovie(
            @PathVariable Long movieId) {

        return showService.getShowsByMovie(movieId);
    }

    // ============================================================
    // GET SHOWS BY THEATRE
    // PUBLIC
    // ============================================================

    @GetMapping("/theatre/{theatreId}")
    public List<Show> getShowsByTheatre(
            @PathVariable Long theatreId) {

        return showService.getShowsByTheatre(
                theatreId
        );
    }

    // ============================================================
    // GET SHOWS BY DATE
    // PUBLIC
    // ============================================================

    @GetMapping("/date/{date}")
    public List<Show> getShowsByDate(
            @PathVariable LocalDate date) {

        return showService.getShowsByDate(date);
    }

    // ============================================================
    // GET SHOWS BY MOVIE + DATE
    // PUBLIC
    // ============================================================

    @GetMapping("/movie/{movieId}/date/{date}")
    public List<Show> getShowsByMovieAndDate(
            @PathVariable Long movieId,
            @PathVariable LocalDate date) {

        return showService.getShowsByMovieAndDate(
                movieId,
                date
        );
    }

    // ============================================================
    // GET SHOWS BY THEATRE + DATE
    // PUBLIC
    // ============================================================

    @GetMapping("/theatre/{theatreId}/date/{date}")
    public List<Show> getShowsByTheatreAndDate(
            @PathVariable Long theatreId,
            @PathVariable LocalDate date) {

        return showService.getShowsByTheatreAndDate(
                theatreId,
                date
        );
    }

    // ============================================================
    // GET SEATS FOR SHOW
    // PUBLIC
    // ============================================================

    @GetMapping("/{showId}/seats")
    public List<Seat> getSeatsByShow(
            @PathVariable Long showId) {

        return showService.getSeatsByShow(showId);
    }
}