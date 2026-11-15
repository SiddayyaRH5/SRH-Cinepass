package com.srh.cinepass.entity;

import com.srh.cinepass.util.BusinessTime;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A single admission credential for a confirmed booking.
 *
 * <p>The QR code contains only {@link #verificationToken}. That token is an
 * opaque 256-bit value generated with {@link java.security.SecureRandom}; it is
 * never derived from a database identifier and never returned to customers.</p>
 */
@Entity
@Table(
        name = "tickets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tickets_booking", columnNames = "booking_id"),
                @UniqueConstraint(name = "uk_tickets_verification_token", columnNames = "verification_token"),
                @UniqueConstraint(name = "uk_tickets_reference", columnNames = "ticket_reference")
        },
        indexes = {
                @Index(name = "idx_tickets_status", columnList = "status"),
                @Index(name = "idx_tickets_created_at", columnList = "created_at"),
                @Index(name = "idx_tickets_verified_at", columnList = "verified_at")
        }
)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "verification_token", nullable = false, length = 88)
    private String verificationToken;

    /** Customer-facing, non-secret reference used on tickets and in staff UI. */
    @Column(name = "ticket_reference", nullable = false, length = 32)
    private String ticketReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.BOOKED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** Guards against lost updates even if a database lock is not honoured. */
    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = BusinessTime.now();
        }
        if (status == null) {
            status = TicketStatus.BOOKED;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getTicketReference() {
        return ticketReference;
    }

    public void setTicketReference(String ticketReference) {
        this.ticketReference = ticketReference;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getVersion() {
        return version;
    }
}
