package com.srh.cinepass.dto;

/**
 * Safe, non-secret view of the automation configuration.
 *
 * <p>Only boolean flags, numeric knobs and the timezone are exposed. No
 * credential, token, secret or password is ever included.</p>
 *
 * @param schedulerEnabled       whether the daily automation job runs
 * @param cleanupEnabled         whether the show-expiry job runs
 * @param tmdbSyncEnabled       whether the TMDB catalogue synchronisation runs
 * @param retirementEnabled     whether automatic movie retirement runs
 * @param ticketBackfillEnabled whether tickets are backfilled on startup
 * @param horizonDays           number of calendar dates in the rolling window
 * @param timezone              business timezone of the scheduler
 */
public record AutomationConfigStatus(
        boolean schedulerEnabled,
        boolean cleanupEnabled,
        boolean tmdbSyncEnabled,
        boolean retirementEnabled,
        boolean ticketBackfillEnabled,
        int horizonDays,
        String timezone) {
}
