package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Theatre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    boolean existsByNameIgnoreCaseAndLocationId(
            String name,
            Long locationId
    );

    boolean existsByNameIgnoreCaseAndLocationIdAndIdNot(
            String name,
            Long locationId,
            Long id
    );

    List<Theatre> findByLocationId(Long locationId);

    List<Theatre> findByOwnerId(Long ownerId);

    /**
     * Theatres the scheduler may create shows for: a positive seat count, a
     * location, and either no owner (admin-created) or an owner whose theatre
     * verification is APPROVED. Pending/rejected owners are excluded.
     */
    @Query("""
            SELECT t FROM Theatre t
            WHERE t.totalSeats > 0
              AND t.location IS NOT NULL
              AND (t.owner IS NULL OR EXISTS (
                    SELECT v.id FROM TheatreVerification v
                    WHERE v.user = t.owner
                      AND v.status = 'APPROVED'
              ))
            ORDER BY t.id ASC
            """)
    List<Theatre> findEligibleForShows();
}
