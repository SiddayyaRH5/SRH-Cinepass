package com.srh.cinepass.repository;

import com.srh.cinepass.entity.ShowGenerationLease;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ShowGenerationLeaseRepository
        extends JpaRepository<ShowGenerationLease, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ShowGenerationLease l WHERE l.id = :id")
    Optional<ShowGenerationLease> findByIdForUpdate(@Param("id") Long id);

    /**
     * Atomically takes the lease when it is free or already expired. A single
     * conditional UPDATE is safe across instances because the database decides
     * the winner. Returns 1 when this caller owns the lease.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ShowGenerationLease l
            SET l.owner = :owner,
                l.lockedUntil = :lockedUntil,
                l.updatedAt = :now
            WHERE l.id = :id
              AND (l.lockedUntil IS NULL OR l.lockedUntil < :now)
            """)
    int tryAcquire(
            @Param("id") Long id,
            @Param("owner") String owner,
            @Param("lockedUntil") LocalDateTime lockedUntil,
            @Param("now") LocalDateTime now);

    /**
     * Releases only a lease still owned by the caller, so a stale instance can
     * never release a lease that was taken over by another instance.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ShowGenerationLease l
            SET l.lockedUntil = :now,
                l.updatedAt = :now
            WHERE l.id = :id
              AND l.owner = :owner
            """)
    int release(
            @Param("id") Long id,
            @Param("owner") String owner,
            @Param("now") LocalDateTime now);
}
