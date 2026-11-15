package com.srh.cinepass.dto;

import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Theatre;

/**
 * Public, non-sensitive representation of a theatre.
 *
 * <p>The owner account association and any KYC data are deliberately excluded;
 * public catalogue clients only need the theatre identity and location.</p>
 */
public record TheatreResponse(
        Long id,
        String name,
        Integer totalSeats,
        LocationResponse location) {

    public static TheatreResponse from(Theatre theatre) {

        if (theatre == null) {
            return null;
        }

        return new TheatreResponse(
                theatre.getId(),
                theatre.getName(),
                theatre.getTotalSeats(),
                LocationResponse.from(theatre.getLocation()));
    }

    public record LocationResponse(
            Long id,
            String city,
            String state,
            String country) {

        public static LocationResponse from(Location location) {

            if (location == null) {
                return null;
            }

            return new LocationResponse(
                    location.getId(),
                    location.getCity(),
                    location.getState(),
                    location.getCountry());
        }
    }
}
