
package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.service.TmdbSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/tmdb")
public class TmdbSyncController {

    private final TmdbSyncService syncService;

    @Value("${tmdb.sync-key:}")
    private String syncKey;

    public TmdbSyncController(TmdbSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync/{movieId}")
    public ResponseEntity<?> syncMovie(
            @PathVariable Long movieId,
            @RequestParam Long tmdbId,
            @RequestHeader(value = "X-SYNC-KEY", required = false) String suppliedKey) {

        if (syncKey == null || syncKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("TMDB sync key is not configured");
        }

        if (suppliedKey == null ||
                !MessageDigest.isEqual(
                        syncKey.getBytes(StandardCharsets.UTF_8),
                        suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Invalid sync key");
        }

        Movie updatedMovie = syncService.syncMovie(movieId, tmdbId);
        return ResponseEntity.ok(updatedMovie);
    }
}
