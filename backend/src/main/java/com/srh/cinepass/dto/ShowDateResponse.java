package com.srh.cinepass.dto;

import java.time.LocalDate;

/**
 * A calendar date (IST) that still has at least one bookable show.
 */
public record ShowDateResponse(LocalDate date) {
}
