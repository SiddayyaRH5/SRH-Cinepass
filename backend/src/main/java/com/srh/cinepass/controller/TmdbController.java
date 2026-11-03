package com.srh.cinepass.controller;

import tools.jackson.databind.JsonNode;
import com.srh.cinepass.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    private final TmdbService tmdbService;

    public TmdbController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    // Search TMDB movies (read-only)
    @GetMapping("/search")
    public ResponseEntity<JsonNode> searchMovies(
            @RequestParam String query) {

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                tmdbService.searchMovies(query));
    }
}