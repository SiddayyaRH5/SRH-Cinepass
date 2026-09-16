package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Show;
import com.srh.cinepass.service.ShowService;

import org.springframework.http.ResponseEntity;
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

    // ==========================================
    // GET ALL SHOWS
    // ==========================================

    @GetMapping
    public List<Show> getAllShows() {
        return showService.getAllShows();
    }

    // ==========================================
    // GET SHOW BY ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<Show> getShowById(
            @PathVariable Long id) {

        return showService.getShowById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // ADD SHOW
    // ==========================================

    @PostMapping
    public ResponseEntity<Show> addShow(
            @RequestBody Show show) {

        Show savedShow = showService.addShow(show);

        return ResponseEntity.ok(savedShow);
    }

    // ==========================================
    // UPDATE SHOW
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<Show> updateShow(
            @PathVariable Long id,
            @RequestBody Show showDetails) {

        try {

            Show updatedShow =
                    showService.updateShow(id, showDetails);

            return ResponseEntity.ok(updatedShow);

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    // ==========================================
    // DELETE SHOW
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(
            @PathVariable Long id) {

        try {

            showService.deleteShow(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    // ==========================================
    // GET SHOWS BY MOVIE
    // ==========================================

    @GetMapping("/movie/{movieId}")
    public List<Show> getShowsByMovie(
            @PathVariable Long movieId) {

        return showService.getShowsByMovie(movieId);
    }

    // ==========================================
    // GET SHOWS BY THEATRE
    // ==========================================

    @GetMapping("/theatre/{theatreId}")
    public List<Show> getShowsByTheatre(
            @PathVariable Long theatreId) {

        return showService.getShowsByTheatre(theatreId);
    }

    // ==========================================
    // GET SHOWS BY DATE
    // ==========================================

    @GetMapping("/date/{date}")
    public List<Show> getShowsByDate(
            @PathVariable LocalDate date) {

        return showService.getShowsByDate(date);
    }

    // ==========================================
    // GET SHOWS BY MOVIE AND DATE
    // ==========================================

    @GetMapping("/movie/{movieId}/date/{date}")
    public List<Show> getShowsByMovieAndDate(
            @PathVariable Long movieId,
            @PathVariable LocalDate date) {

        return showService.getShowsByMovieAndDate(
                movieId,
                date
        );
    }

    // ==========================================
    // GET SHOWS BY THEATRE AND DATE
    // ==========================================

    @GetMapping("/theatre/{theatreId}/date/{date}")
    public List<Show> getShowsByTheatreAndDate(
            @PathVariable Long theatreId,
            @PathVariable LocalDate date) {

        return showService.getShowsByTheatreAndDate(
                theatreId,
                date
        );
    }

    // ==========================================
    // GET SEATS FOR SHOW
    // ==========================================

    @GetMapping("/{showId}/seats")
    public List<com.srh.cinepass.entity.Seat> getSeatsByShow(
            @PathVariable Long showId) {

        return showService.getSeatsByShow(showId);
    }
}