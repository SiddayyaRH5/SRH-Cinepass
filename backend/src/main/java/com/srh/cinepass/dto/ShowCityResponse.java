package com.srh.cinepass.dto;

import com.srh.cinepass.entity.Location;

/**
 * A city that currently has bookable shows for the requested movie.
 */
public record ShowCityResponse(
        Long id,
        String city,
        String state,
        String country) {

    public static ShowCityResponse from(Location location) {

        if (location == null) {
            return null;
        }

        return new ShowCityResponse(
                location.getId(),
                location.getCity(),
                location.getState(),
                location.getCountry());
    }
}
