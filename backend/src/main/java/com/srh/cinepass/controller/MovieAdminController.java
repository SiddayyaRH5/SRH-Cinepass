package com.srh.cinepass.controller;

import com.srh.cinepass.dto.AutomationRunView;
import com.srh.cinepass.dto.MovieAdminView;
import com.srh.cinepass.dto.MovieSyncStatusView;
import com.srh.cinepass.entity.AutomationRun;
import com.srh.cinepass.repository.AutomationRunRepository;
import com.srh.cinepass.service.DailyAutomationService;
import com.srh.cinepass.service.MovieAdminService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Administrator API for the automated movie catalogue.
 *
 * <p>Every endpoint below sits under {@code /api/admin/**}, which the
 * security configuration already restricts to the ADMIN role. Nothing here
 * weakens authentication or authorisation, and no endpoint is public.</p>
 */
@RestController
@RequestMapping("/api/admin")
public class MovieAdminController {

    private final MovieAdminService movieAdminService;
    private final DailyAutomationService dailyAutomationService;
    private final AutomationRunRepository automationRunRepository;

    public MovieAdminController(
            MovieAdminService movieAdminService,
            DailyAutomationService dailyAutomationService,
            AutomationRunRepository automationRunRepository) {

        this.movieAdminService = movieAdminService;
        this.dailyAutomationService = dailyAutomationService;
        this.automationRunRepository = automationRunRepository;
    }

    // ============================================================
    // Catalogue inspection
    // ============================================================

    /** Every movie, including the retired ones. */
    @GetMapping("/movies")
    public List<MovieAdminView> listMovies() {
        return movieAdminService.listAllMovies();
    }

    /** Only the movies that the automatic TMDB synchronisation manages. */
    @GetMapping("/movies/imported")
    public List<MovieAdminView> listImportedMovies() {
        return movieAdminService.listImportedMovies();
    }

    /** Synchronisation status, including duplicate and missing TMDB ids. */
    @GetMapping("/movies/sync-status")
    public MovieSyncStatusView syncStatus() {
        return movieAdminService.syncStatus();
    }

    // ============================================================
    // Catalogue control
    // ============================================================

    /** Includes or excludes a movie from automatic show generation. */
    @PostMapping("/movies/{id}/exclude")
    public ResponseEntity<?> exclude(
            @PathVariable Long id,
            @RequestBody(required = false) ExcludeRequest request) {

        boolean excluded = request == null || request.excluded() == null
                ? true
                : request.excluded();

        try {
            return ResponseEntity.ok(
                    movieAdminService.setExcluded(id, excluded, request == null ? null : request.reason()));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    /** Protects a movie from the automatic retirement job. */
    @PostMapping("/movies/{id}/protect")
    public ResponseEntity<?> protect(
            @PathVariable Long id,
            @RequestBody(required = false) ProtectRequest request) {

        boolean protectedMovie = request == null || request.protectedMovie() == null
                ? true
                : request.protectedMovie();

        try {
            return ResponseEntity.ok(
                    movieAdminService.setProtected(id, protectedMovie));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    /** Extends the active period of a movie. */
    @PostMapping("/movies/{id}/extend")
    public ResponseEntity<?> extend(
            @PathVariable Long id,
            @RequestBody ExtendRequest request) {

        if (request == null || request.activeUntil() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "activeUntil is required (YYYY-MM-DD)"));
        }

        LocalDate activeUntil;

        try {
            activeUntil = LocalDate.parse(request.activeUntil());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "activeUntil must be a valid date (YYYY-MM-DD)"));
        }

        try {
            return ResponseEntity.ok(
                    movieAdminService.extendActivePeriod(id, activeUntil));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    /** Brings an archived movie back into the active catalogue. */
    @PostMapping("/movies/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {

        try {
            return ResponseEntity.ok(movieAdminService.reactivate(id));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    /** Retires a movie manually. The row and its history are preserved. */
    @PostMapping("/movies/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable Long id) {

        try {
            return ResponseEntity.ok(movieAdminService.archive(id));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex);
        }
    }

    // ============================================================
    // Automation runs
    // ============================================================

    /** The most recent daily automation runs. */
    @GetMapping("/automation/runs")
    public List<AutomationRunView> listRuns() {
        return automationRunRepository.findTop50ByOrderByStartedAtDesc().stream()
                .map(AutomationRunView::from)
                .toList();
    }

    /** Triggers the daily automation immediately. */
    @PostMapping("/automation/run")
    public AutomationRunView runAutomation() {
        AutomationRun run = dailyAutomationService.runDailyAutomation(
                AutomationRun.TRIGGER_MANUAL);

        return AutomationRunView.from(run);
    }

    private static ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    // ============================================================
    // Request bodies
    // ============================================================

    /**
     * @param excluded  whether the scheduler must skip the movie
     * @param reason    optional note describing why
     */
    public record ExcludeRequest(Boolean excluded, String reason) {
    }

    /**
     * @param protectedMovie whether automatic retirement must skip the movie
     */
    public record ProtectRequest(Boolean protectedMovie) {
    }

    /**
     * @param activeUntil the last date the movie stays active, YYYY-MM-DD
     */
    public record ExtendRequest(String activeUntil) {
    }
}
