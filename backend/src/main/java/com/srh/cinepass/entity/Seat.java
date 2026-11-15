
package com.srh.cinepass.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srh.cinepass.util.BusinessTime;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String seatNumber;

    private boolean booked = false;

    // Temporary payment reservation information
    private Long reservedByBookingId;

    private LocalDateTime reservedUntil;

    @ManyToOne
    @JoinColumn(name = "show_id")
    private Show show;

    public Seat() {
    }

    public Long getId() {
        return id;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    // Existing frontend continues receiving "booked".
    // An active temporary reservation also appears unavailable.
    public boolean isBooked() {
        return isBookedAt(BusinessTime.now());
    }

    /**
     * Availability at an explicit business time. Service-layer callers pass
     * the injected IST clock so tests and recovery logic are deterministic.
     */
    public boolean isBookedAt(LocalDateTime now) {
        return booked || (reservedUntil != null &&
                now != null &&
                reservedUntil.isAfter(now));
    }

    /**
     * True only when the seat has been permanently assigned to a confirmed
     * booking. Unlike {@link #isBooked()}, this does not include an active
     * temporary reservation.
     */
    public boolean isPermanentlyBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    @JsonIgnore
    public Long getReservedByBookingId() {
        return reservedByBookingId;
    }

    public void setReservedByBookingId(Long reservedByBookingId) {
        this.reservedByBookingId = reservedByBookingId;
    }

    @JsonIgnore
    public LocalDateTime getReservedUntil() {
        return reservedUntil;
    }

    public void setReservedUntil(LocalDateTime reservedUntil) {
        this.reservedUntil = reservedUntil;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }
}
