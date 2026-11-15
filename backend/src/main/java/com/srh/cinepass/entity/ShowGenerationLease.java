package com.srh.cinepass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Single-row lease used to serialise show generation across application
 * instances. The row is created on first use; its primary key is fixed at
 * {@link #SINGLETON_ID} so two instances cannot both "own" the job.
 *
 * <p>A crashed instance leaves a lease behind, so the lease always carries an
 * expiry ({@code lockedUntil}). A later run may take over once it has passed.</p>
 */
@Entity
@Table(name = "show_generation_lease")
public class ShowGenerationLease {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "owner", length = 120)
    private String owner;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ShowGenerationLease() {
    }

    public ShowGenerationLease(
            String owner,
            LocalDateTime lockedUntil,
            LocalDateTime updatedAt) {

        this.id = SINGLETON_ID;
        this.owner = owner;
        this.lockedUntil = lockedUntil;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
