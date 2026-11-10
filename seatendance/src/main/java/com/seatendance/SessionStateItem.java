package com.seatendance;

import java.time.LocalDate;

// Stores one calendar date and its session state.
final class SessionStateItem {
    final LocalDate date;
    final String state;

    // Saves the date and state text.
    SessionStateItem(LocalDate date, String state) {
        this.date = date;
        this.state = safe(state);
    }

    // Trims text and safely turns null into an empty string.
    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
