package com.srh.cinepass.controller;

import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Liveness probe: Returns UP immediately without checking the database.
     * Prevents Render from killing container startup during cold boots.
     */
    @GetMapping("/live")
    public ResponseEntity> live() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "SRH-CinePass",
            "probe", "liveness",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Readiness probe: Verifies database connectivity with a strict check.
     */
    @GetMapping("/ready")
    public ResponseEntity> ready() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            statement.setQueryTimeout(2); // Strict 2-second timeout
            statement.execute("SELECT 1");

            return ResponseEntity.ok(Map.of(
                "status", "READY",
                "database", "CONNECTED",
                "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "NOT_READY",
                "database", "DISCONNECTED",
                "error", e.getMessage(),
                "timestamp", Instant.now().toString()
            ));
        }
    }

    // Keep legacy endpoint for backward compatibility
    @GetMapping
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "SRH-CinePass", "timestamp", Instant.now().toString());
    }
}