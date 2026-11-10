package com.seatendance;

// Stores one student's saved attendance mark.
final class AttendanceMarkItem {
    final String studentId;
    final DatabaseHandler.AttendanceMark mark;

    // Saves the student id and attendance mark.
    AttendanceMarkItem(String studentId, DatabaseHandler.AttendanceMark mark) {
        this.studentId = safe(studentId);
        this.mark = mark;
    }

    // Trims text and safely turns null into an empty string.
    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
