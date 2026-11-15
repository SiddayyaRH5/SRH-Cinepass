package com.srh.cinepass.entity;

/**
 * How a movie row is managed.
 *
 * <p>The automatic TMDB catalogue sync only refreshes metadata of
 * {@link #TMDB} movies. {@link #MANUAL} movies are created by an
 * administrator or the demo seeder and are never overwritten by an automated
 * run. A {@code null} source on a legacy row is treated as
 * {@link #MANUAL}.</p>
 */
public enum MovieSource {

    /**
     * Created and owned by an administrator. Manual movies are never
     * auto-updated from TMDB.
     */
    MANUAL,

    /**
     * Imported or refreshed by the automatic TMDB catalogue synchronisation.
     */
    TMDB
}
