package com.srh.cinepass.support;

import com.srh.cinepass.util.BusinessTime;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Test clock that can be moved to an exact instant. The production
 * {@link Clock} bean is replaced by this instance through {@link TestClockConfig}
 * so time-dependent behaviour is deterministic without changing the JVM
 * default timezone.
 */
public class MutableClock extends Clock {

    private volatile Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public void set(LocalDateTime businessDateTime) {
        this.instant = businessDateTime.atZone(zone).toInstant();
    }

    public void advance(Duration duration) {
        this.instant = this.instant.plus(duration);
    }

    public ZoneId businessZone() {
        return BusinessTime.ZONE;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
