package com.srh.cinepass.service;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Cacheable(cacheNames = "locations")
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }

    @CacheEvict(cacheNames = "locations", allEntries = true)
    public Location addLocation(Location location) {
        return locationRepository.save(location);
    }

    @CacheEvict(cacheNames = "locations", allEntries = true)
    public Location updateLocation(Long id, Location locationDetails) {

        Location location = locationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Location not found with id: " + id));

        location.setCity(locationDetails.getCity());
        location.setState(locationDetails.getState());
        location.setCountry(locationDetails.getCountry());

        return locationRepository.save(location);
    }

    @CacheEvict(cacheNames = "locations", allEntries = true)
    public void deleteLocation(Long id) {

        if (!locationRepository.existsById(id)) {
            throw new RuntimeException(
                    "Location not found with id: " + id);
        }

        locationRepository.deleteById(id);
    }
}