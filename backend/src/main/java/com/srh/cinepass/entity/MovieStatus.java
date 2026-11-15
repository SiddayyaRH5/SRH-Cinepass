package com.srh.cinepass.entity;

/**
 * Customer-facing lifecycle state of a movie.
 *
 * <p>Retirement never deletes a movie. It moves the row to
 * {@link #ARCHIVED}, which hides it from the active catalogue and blocks new
 * shows and new bookings while every historical record (shows, bookings,
 * tickets, seats, payments) stays exactly where it is.</p>
 */
public enum MovieStatus {

    /**
     * The movie is part of the active catalogue and is eligible for automatic
     * show generation. A {@code null} status on a legacy row is treated as
     * {@code ACTIVE} so pre-existing movies keep working unchanged.
     */
    ACTIVE,

    /**
     * The movie has been retired from the active catalogue. Existing shows
     * are preserved and are not cancelled; no new shows and no new bookings
     * are allowed.
     */
    ARCHIVED
}
