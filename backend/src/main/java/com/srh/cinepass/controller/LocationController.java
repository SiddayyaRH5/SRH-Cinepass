package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin(origins = "http://localhost:5173")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<Location> getAllLocations() {
        return locationService.getAllLocations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocationById(
            @PathVariable Long id) {

        return locationService.getLocationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Location> addLocation(
            @RequestBody Location location) {

        Location savedLocation =
                locationService.addLocation(location);

        return ResponseEntity.ok(savedLocation);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(
            @PathVariable Long id,
            @RequestBody Location locationDetails) {

        try {
            Location updatedLocation =
                    locationService.updateLocation(
                            id, locationDetails);

            return ResponseEntity.ok(updatedLocation);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(
            @PathVariable Long id) {

        try {
            locationService.deleteLocation(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}