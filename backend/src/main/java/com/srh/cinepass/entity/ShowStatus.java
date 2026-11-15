package com.srh.cinepass.entity;

/**
 * Lifecycle state of a show.
 *
 * <p>The column is nullable for backward compatibility with shows created
 * before this feature existed. A {@code null} status is treated as
 * {@link #SCHEDULED} everywhere in the application.</p>
 */
public enum ShowStatus {

    /** Scheduled and (unless its booking window has closed) bookable. */
    SCHEDULED,

    /** Started more than the configured grace period ago. Never bookable. */
    EXPIRED,

    /** Explicitly completed by an operator. Never bookable. */
    COMPLETED,

    /** Explicitly cancelled by an operator. Never bookable. */
    CANCELLED
}
