package com.srh.cinepass.dto;

/**
 * A movie returned by one of the TMDB discovery lists.
 *
 * <p>A candidate is only a candidate. It becomes a catalogue entry once its
 * Indian theatrical release date has been confirmed through the TMDB release
 * dates endpoint.</p>
 *
 * @param tmdbId           stable TMDB movie identifier
 * @param title            display title
 * @param overview         synopsis, possibly empty
 * @param posterPath       TMDB relative poster path, possibly empty
 * @param backdropPath     TMDB relative backdrop path, possibly empty
 * @param originalLanguage ISO 639-1 original language code, possibly empty
 * @param rating           TMDB vote average, {@code 0} when unknown
 */
public record TmdbMovieCandidate(
        long tmdbId,
        String title,
        String overview,
        String posterPath,
        String backdropPath,
        String originalLanguage,
        double rating) {
}
