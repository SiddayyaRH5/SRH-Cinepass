package com.srh.cinepass.service;

import com.srh.cinepass.entity.ShowGenerationLease;
import com.srh.cinepass.repository.ShowGenerationLeaseRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Cross-instance mutex for show generation.
 *
 * <p>A single lease row is claimed with a conditional UPDATE, so the database
 * decides which instance runs. The lease expires on its own, which means a
 * crashed instance cannot block generation forever. This is a best-effort
 * optimisation on top of the database uniqueness constraint: even without the
 * lease, duplicate shows are impossible.</p>
 */
@Service
public class ShowGenerationLeaseService {

    private static final long LEASE_ID = ShowGenerationLease.SINGLETON_ID;

    private final ShowGenerationLeaseRepository leaseRepository;
    private final Clock clock;
    private final long leaseMinutes;

    public ShowGenerationLeaseService(
            ShowGenerationLeaseRepository leaseRepository,
            Clock clock,
            @Value("${app.shows.scheduler.lease-minutes:30}") long leaseMinutes) {

        this.leaseRepository = leaseRepository;
        this.clock = clock;
        this.leaseMinutes = Math.max(1, leaseMinutes);
    }

    /**
     * Creates the singleton lease row when it does not exist yet.
     *
     * <p>Deliberately not transactional at this level: the repository call runs
     * in its own transaction, so losing an insert race to another instance is
     * an isolated, harmless failure.</p>
     */
    public void ensureLeaseRow() {

        if (leaseRepository.existsById(LEASE_ID)) {
            return;
        }

        try {
            leaseRepository.saveAndFlush(
                    new ShowGenerationLease(
                            null,
                            null,
                            LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException ignored) {
            // Another instance created the row first.
        }
    }

    /**
     * @return true when this caller now owns the lease.
     */
    @Transactional
    public boolean tryAcquire(String owner) {

        LocalDateTime now = LocalDateTime.now(clock);

        return leaseRepository.tryAcquire(
                LEASE_ID,
                owner,
                now.plusMinutes(leaseMinutes),
                now) == 1;
    }

    /**
     * Releases the lease only if it is still owned by {@code owner}.
     *
     * <p>The release timestamp is deliberately one second in the past so the
     * very next acquisition (even within the same wall-clock second, as in
     * tests with a fixed clock) succeeds immediately.</p>
     */
    @Transactional
    public void release(String owner) {

        leaseRepository.release(
                LEASE_ID,
                owner,
                LocalDateTime.now(clock).minusSeconds(1));
    }
}
