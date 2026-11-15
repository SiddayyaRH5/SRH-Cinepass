package com.srh.cinepass.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The business operates in Indian Standard Time. Show times, reservation
 * expiries and ticket admission windows are stored as IST wall-clock
 * {@link LocalDateTime} values, so "now" must always be evaluated in the same
 * zone regardless of the operating system or container default (Render runs
 * the JVM in UTC).
 */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private BusinessTime() {
    }

    public static Clock clock() {
        return Clock.system(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
