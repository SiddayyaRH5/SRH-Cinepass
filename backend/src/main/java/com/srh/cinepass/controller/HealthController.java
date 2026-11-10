package com.srh.cinepass.controller;

import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/health/live")
    public Map<String, Object> live() {
        return Map.of(
            "status", "UP",
            "service", "SRH-CinePass",
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/api/health/ready")
    public Map<String, Object> ready() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(2);
            statement.execute("SELECT 1");
            return Map.of(
                "status", "READY",
                "database", "CONNECTED",
                "timestamp", Instant.now().toString()
            );
        } catch (Exception e) {
            return Map.of(
                "status", "NOT_READY",
                "database", "DISCONNECTED",
                "timestamp", Instant.now().toString()
            );
        }
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "SRH-CinePass", "timestamp", Instant.now().toString());
    }
}