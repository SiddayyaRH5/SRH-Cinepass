package com.srh.cinepass.dto;

import com.srh.cinepass.entity.Theatre;

/**
 * A theatre in a city that currently has bookable shows for the movie.
 */
public record ShowTheatreResponse(
        Long id,
        String name,
        String city,
        Integer totalSeats) {

    public static ShowTheatreResponse from(Theatre theatre) {

        if (theatre == null) {
            return null;
        }

        return new ShowTheatreResponse(
                theatre.getId(),
                theatre.getName(),
                theatre.getLocation() != null
                        ? theatre.getLocation().getCity()
                        : null,
                theatre.getTotalSeats());
    }
}
