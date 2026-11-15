package com.srh.cinepass.util;

/**
 * Converts the backend seat identifier (S77) into the seat label shown in the
 * existing SRH CinePass seat map (H7).
 */
public final class SeatLabels {

    private SeatLabels() {
    }

    public static String toDisplayLabel(String seatNumber) {
        if (seatNumber == null || seatNumber.isBlank()) {
            return "";
        }

        String value = seatNumber.trim();

        if (!value.matches("^[Ss][0-9]+$")) {
            return value;
        }

        int number = Integer.parseInt(value.substring(1));

        if (number < 1) {
            return value;
        }

        int row = (number - 1) / 10;
        int seat = ((number - 1) % 10) + 1;

        return String.valueOf((char) ('A' + row)) + seat;
    }
}
