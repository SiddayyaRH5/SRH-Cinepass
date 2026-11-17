package com.srh.cinepass.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Full metadata for one movie, including the Indian theatrical release date,
 * the trailer key and the principal cast.
 *
 * @param tmdbId                     stable TMDB movie identifier
 * @param title                      display title
 * @param overview                   synopsis, possibly empty
 * @param posterUrl                  absolute poster URL, possibly empty
 * @param backdropUrl                absolute backdrop URL, possibly empty
 * @param originalLanguage           ISO 639-1 original language code, possibly empty
 * @param genres                     comma separated genre names, possibly empty
 * @param rating                     TMDB vote average, {@code null} when unknown
 * @param runtime                    runtime in minutes, {@code null} when unknown
 * @param indiaTheatricalReleaseDate the confirmed Indian theatrical release date,
 *                                   {@code null} when TMDB has no Indian theatrical date
 * @param trailerKey                 YouTube video key for the trailer, {@code null} when unavailable
 * @param cast                       principal cast members, possibly empty
 */
public record TmdbMovieDetails(
        long tmdbId,
        String title,
        String overview,
        String posterUrl,
        String backdropUrl,
        String originalLanguage,
        String genres,
        Double rating,
        Integer runtime,
        LocalDate indiaTheatricalReleaseDate,
        String trailerKey,
        List<TmdbCastMember> cast) {
}
