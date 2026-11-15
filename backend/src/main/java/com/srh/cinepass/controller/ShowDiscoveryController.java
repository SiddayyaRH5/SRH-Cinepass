package com.srh.cinepass.controller;

import com.srh.cinepass.dto.ShowAvailabilityResponse;
import com.srh.cinepass.dto.ShowCityResponse;
import com.srh.cinepass.dto.ShowDateResponse;
import com.srh.cinepass.dto.ShowTheatreResponse;
import com.srh.cinepass.dto.ShowTimeResponse;
import com.srh.cinepass.service.ShowDiscoveryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Public movie-based discovery endpoints used by the movie-detail booking
 * flow. The existing {@code /api/shows} endpoints are unchanged; these are
 * additive and read-only.
 */
@RestController
@RequestMapping("/api/shows")
public class ShowDiscoveryController {

    private final ShowDiscoveryService discoveryService;

    public ShowDiscoveryController(ShowDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/discover/movies/{movieId}/cities")
    public List<ShowCityResponse> getCities(
            @PathVariable Long movieId) {

        return discoveryService.getCitiesForMovie(movieId);
    }

    @GetMapping("/discover/movies/{movieId}/cities/{locationId}/theatres")
    public List<ShowTheatreResponse> getTheatres(
            @PathVariable Long movieId,
            @PathVariable Long locationId) {

        return discoveryService.getTheatresForMovieInCity(
                movieId,
                locationId);
    }

    @GetMapping("/discover/movies/{movieId}/theatres/{theatreId}/dates")
    public List<ShowDateResponse> getDates(
            @PathVariable Long movieId,
            @PathVariable Long theatreId) {

        return discoveryService.getDatesForMovieAtTheatre(
                movieId,
                theatreId);
    }

    @GetMapping("/discover/movies/{movieId}/theatres/{theatreId}/dates/{date}")
    public List<ShowTimeResponse> getShowTimes(
            @PathVariable Long movieId,
            @PathVariable Long theatreId,
            @PathVariable LocalDate date) {

        return discoveryService.getShowTimes(
                movieId,
                theatreId,
                date);
    }

    @GetMapping("/{showId}/availability")
    public ResponseEntity<ShowAvailabilityResponse> getAvailability(
            @PathVariable Long showId) {

        try {

            return ResponseEntity.ok(
                    discoveryService.getAvailability(showId));

        } catch (RuntimeException ex) {

            return ResponseEntity.notFound().build();
        }
    }
}
