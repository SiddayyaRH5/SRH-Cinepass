package com.srh.cinepass.controller;

import com.srh.cinepass.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tmdb")
public class TmdbAdminController {

    private final TmdbService tmdbService;

    public TmdbAdminController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @PostMapping("/sync-posters")
    public Map<String, Object> syncPosters() {
        try {
            int count = tmdbService.syncAllMoviePosters();
            return Map.of(
                "success", true,
                "message", "Successfully synchronized posters for " + count + " movies."
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        }
    }
}