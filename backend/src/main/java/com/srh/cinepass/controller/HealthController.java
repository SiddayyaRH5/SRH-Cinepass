package com.srh.cinepass.controller;

import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Liveness must stay independent of the database. */
    @GetMapping({"/health", "/api/health/live"})
    public Map<String, Object> live() {
        return Map.<String, Object>of(
                "status", "UP",
                "service", "SRH-CinePass",
                "timestamp", Instant.now().toString());
    }

    /** Readiness borrows one pooled connection and performs only JDBC validation. */
    @GetMapping("/api/health/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        try (var connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            if (valid) {
                return ResponseEntity.ok(Map.<String, Object>of(
                        "status", "READY",
                        "database", "UP",
                        "timestamp", Instant.now().toString()));
            }
        } catch (Exception ignored) {
            // Do not expose database hostnames, credentials, or driver details.
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.<String, Object>of(
                "status", "NOT_READY",
                "database", "UNAVAILABLE",
                "timestamp", Instant.now().toString()));
    }
}
