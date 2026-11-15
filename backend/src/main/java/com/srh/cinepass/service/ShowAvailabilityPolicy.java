package com.srh.cinepass.service;

import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.ShowStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Single source of truth for "can a customer still book this show?".
 *
 * <p>Two independent rules are applied:</p>
 * <ol>
 *   <li>Lifecycle: only SCHEDULED (or legacy null) shows are bookable.</li>
 *   <li>Booking window: sales stop a configurable number of minutes before
 *       the show starts. This is separate from the post-start expiry rule.</li>
 * </ol>
 *
 * <p>All comparisons use the injected IST clock, never the JVM default zone.</p>
 */
@Component
public class ShowAvailabilityPolicy {

    private final Clock clock;
    private final int closeMinutesBeforeStart;

    public ShowAvailabilityPolicy(
            Clock clock,
            @Value("${app.shows.booking.close-minutes-before-start:15}") int closeMinutesBeforeStart) {

        this.clock = clock;
        this.closeMinutesBeforeStart = Math.max(0, closeMinutesBeforeStart);
    }

    public boolean isBookable(Show show) {
        return isBookable(show, LocalDateTime.now(clock));
    }

    public boolean isBookable(Show show, LocalDateTime now) {

        if (show == null || show.getShowDateTime() == null || now == null) {
            return false;
        }

        ShowStatus status = show.getStatus();

        if (status != null && status != ShowStatus.SCHEDULED) {
            return false;
        }

        LocalDateTime bookingClosesAt =
                show.getShowDateTime().minusMinutes(closeMinutesBeforeStart);

        return bookingClosesAt.isAfter(now);
    }

    /**
     * Start of the earliest bookable instant, used by repository queries so the
     * database does the filtering instead of loading every show into memory.
     */
    public LocalDateTime earliestBookableStart(LocalDateTime now) {
        return now.plusMinutes(closeMinutesBeforeStart);
    }

    public int getCloseMinutesBeforeStart() {
        return closeMinutesBeforeStart;
    }
}
