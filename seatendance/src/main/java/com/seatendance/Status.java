package com.seatendance;

import java.awt.Color;

// Stores the display text, database value, and color for each attendance status.
enum Status {
    UNMARKED("Unmarked", null, StartSessionPanel.GRAY),
    PRESENT("Present", "Present", StartSessionPanel.GREEN),
    LATE("Late", "Late", StartSessionPanel.YELLOW),
    ABSENT("Absent", "Absent", StartSessionPanel.RED),
    EXCUSED("Excused", "Excused", StartSessionPanel.BLUE);

    final String label;
    final String dbValue;
    final Color color;

    // Stores the label, database value, and display color for one status.
    Status(String label, String dbValue, Color color) {
        this.label = label;
        this.dbValue = dbValue;
        this.color = color;
    }

    // Returns the next status used when clicking a student card.
    Status next() {
        switch (this) {
            case UNMARKED:
                return PRESENT;
            case PRESENT:
                return LATE;
            case LATE:
                return ABSENT;
            case ABSENT:
                return EXCUSED;
            case EXCUSED:
            default:
                return UNMARKED;
        }
    }

    // Converts a database status string into a Status enum value.
    static Status fromDb(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNMARKED;
        }
        for (Status status : values()) {
            if (value.equalsIgnoreCase(status.dbValue)) {
                return status;
            }
        }
        return UNMARKED;
    }
}
