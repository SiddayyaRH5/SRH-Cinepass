package com.srh.cinepass.dto;

/**
 * A principal cast member of a movie, as returned by TMDB.
 *
 * @param name        actor name
 * @param character   character name, possibly empty
 * @param profilePath TMDB relative profile image path, possibly empty
 */
public record TmdbCastMember(
        String name,
        String character,
        String profilePath) {
}
