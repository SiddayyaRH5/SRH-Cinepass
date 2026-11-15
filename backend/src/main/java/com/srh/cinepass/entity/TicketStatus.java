package com.srh.cinepass.entity;

/**
 * Lifecycle of a ticket.
 *
 * <p>BOOKED is the only state in which a ticket may be admitted. USED,
 * CANCELLED and EXPIRED are terminal for admission purposes and are retained
 * for audit history.</p>
 */
public enum TicketStatus {
    BOOKED,
    USED,
    CANCELLED,
    EXPIRED
}
