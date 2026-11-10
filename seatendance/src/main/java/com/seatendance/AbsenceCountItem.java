package com.seatendance;

// Stores one student's absence count.
final class AbsenceCountItem {
    final String studentId;
    final int count;

    // Saves the student id and absence count.
    AbsenceCountItem(String studentId, int count) {
        this.studentId = safe(studentId);
        this.count = count;
    }

    // Trims text and safely turns null into an empty string.
    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
