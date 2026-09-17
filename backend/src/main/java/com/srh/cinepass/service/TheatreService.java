package com.srh.cinepass.service;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.LocationRepository;
import com.srh.cinepass.repository.TheatreRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final LocationRepository locationRepository;

    public TheatreService(
            TheatreRepository theatreRepository,
            LocationRepository locationRepository) {

        this.theatreRepository = theatreRepository;
        this.locationRepository = locationRepository;
    }

    // =========================================
    // GET
    // =========================================

    public List<Theatre> getAllTheatres() {
        return theatreRepository.findAll();
    }

    public Optional<Theatre> getTheatreById(Long id) {
        return theatreRepository.findById(id);
    }

    public List<Theatre> getTheatresByLocation(
            Long locationId) {

        return theatreRepository
                .findByLocationId(locationId);
    }

    public List<Theatre> getTheatresByOwner(
            Long ownerId) {

        return theatreRepository
                .findByOwnerId(ownerId);
    }


    // =========================================
    // ADD THEATRE
    // =========================================

    public Theatre addTheatre(
            Theatre theatre,
            User loggedInUser) {

        if (theatre.getName() == null ||
            theatre.getName().isBlank()) {

            throw new RuntimeException(
                "Theatre name is required"
            );
        }

        if (theatre.getTotalSeats() == null ||
            theatre.getTotalSeats() < 10 ||
            theatre.getTotalSeats() > 2000) {

            throw new RuntimeException(
                "Total seats must be between 10 and 2000"
            );
        }

        if (theatre.getLocation() == null ||
            theatre.getLocation().getId() == null) {

            throw new RuntimeException(
                "Location is required"
            );
        }

        Long locationId =
                theatre.getLocation().getId();

        Location location =
                locationRepository
                        .findById(locationId)
                        .orElseThrow(() ->
                            new RuntimeException(
                                "Location not found"
                            )
                        );

        boolean duplicate =
                theatreRepository
                    .existsByNameIgnoreCaseAndLocationId(
                        theatre.getName().trim(),
                        locationId
                    );

        if (duplicate) {

            throw new RuntimeException(
                "A theatre with this name already exists in this city"
            );
        }

        theatre.setName(
                theatre.getName().trim()
        );

        theatre.setLocation(location);

        /*
         * ADMIN-created theatres remain platform-owned.
         * THEATRE_OWNER-created theatres are automatically
         * assigned to that owner.
         */
        if (loggedInUser != null &&
            "THEATRE_OWNER".equalsIgnoreCase(
                loggedInUser.getRole()
            )) {

            theatre.setOwner(loggedInUser);

        } else {

            theatre.setOwner(null);
        }

        return theatreRepository.save(theatre);
    }


    // =========================================
    // UPDATE
    // =========================================

    public Theatre updateTheatre(
            Long id,
            Theatre theatreDetails,
            User loggedInUser) {

        Theatre theatre =
                theatreRepository
                    .findById(id)
                    .orElseThrow(() ->
                        new RuntimeException(
                            "Theatre not found with id: " + id
                        )
                    );

        /*
         * Theatre owners can modify only their own theatre.
         */
        if ("THEATRE_OWNER".equalsIgnoreCase(
                loggedInUser.getRole())) {

            if (theatre.getOwner() == null ||
                !theatre.getOwner()
                        .getId()
                        .equals(loggedInUser.getId())) {

                throw new RuntimeException(
                    "You can only manage your own theatre"
                );
            }
        }

        if (theatreDetails.getName() == null ||
            theatreDetails.getName().isBlank()) {

            throw new RuntimeException(
                "Theatre name is required"
            );
        }

        if (theatreDetails.getTotalSeats() == null ||
            theatreDetails.getTotalSeats() < 10 ||
            theatreDetails.getTotalSeats() > 2000) {

            throw new RuntimeException(
                "Total seats must be between 10 and 2000"
            );
        }

        if (theatreDetails.getLocation() == null ||
            theatreDetails.getLocation().getId() == null) {

            throw new RuntimeException(
                "Location is required"
            );
        }

        Long locationId =
                theatreDetails
                    .getLocation()
                    .getId();

        Location location =
                locationRepository
                    .findById(locationId)
                    .orElseThrow(() ->
                        new RuntimeException(
                            "Location not found"
                        )
                    );

        boolean duplicate =
                theatreRepository
                    .existsByNameIgnoreCaseAndLocationIdAndIdNot(
                        theatreDetails.getName().trim(),
                        locationId,
                        id
                    );

        if (duplicate) {

            throw new RuntimeException(
                "A theatre with this name already exists in this city"
            );
        }

        theatre.setName(
                theatreDetails.getName().trim()
        );

        theatre.setTotalSeats(
                theatreDetails.getTotalSeats()
        );

        theatre.setLocation(location);

        return theatreRepository.save(theatre);
    }


    // =========================================
    // DELETE
    // =========================================

    public void deleteTheatre(
            Long id,
            User loggedInUser) {

        Theatre theatre =
                theatreRepository
                    .findById(id)
                    .orElseThrow(() ->
                        new RuntimeException(
                            "Theatre not found with id: " + id
                        )
                    );

        /*
         * Theatre owners can delete only their own theatre.
         */
        if ("THEATRE_OWNER".equalsIgnoreCase(
                loggedInUser.getRole())) {

            if (theatre.getOwner() == null ||
                !theatre.getOwner()
                        .getId()
                        .equals(loggedInUser.getId())) {

                throw new RuntimeException(
                    "You can only manage your own theatre"
                );
            }
        }

        theatreRepository.delete(theatre);
    }
}