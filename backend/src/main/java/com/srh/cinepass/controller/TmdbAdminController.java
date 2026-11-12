package com.srh.cinepass.controller;

import com.srh.cinepass.service.CatalogRepairService;
import com.srh.cinepass.service.TmdbService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tmdb")
public class TmdbAdminController {

    private final TmdbService tmdbService;
    private final CatalogRepairService catalogRepairService;

    public TmdbAdminController(
        TmdbService tmdbService,
        CatalogRepairService catalogRepairService
    ) {
        this.tmdbService = tmdbService;
        this.catalogRepairService = catalogRepairService;
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
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/repair-catalog")
    public Map<String, Object> repairCatalog() {
        return catalogRepairService.repairCatalog();
    }
}
