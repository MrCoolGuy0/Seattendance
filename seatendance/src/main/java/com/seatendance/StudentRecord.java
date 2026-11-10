package com.seatendance;

import java.time.LocalTime;

// Holds one student row shown in the attendance screen.
final class StudentRecord {
    final String studentId;
    final String name;
    Status status;
    int absenceCount;
    LocalTime timeIn;
    LocalTime timeOut;
    int seatNumber;

    // Stores one student's id, name, status, absence count, and times.
    StudentRecord(String studentId, String name, Status status, int absenceCount,
            LocalTime timeIn, LocalTime timeOut, int seatNumber) {
        this.studentId = safe(studentId);
        this.name = safe(name);
        if (status == null) {
            this.status = Status.UNMARKED;
        } else {
            this.status = status;
        }
        this.absenceCount = Math.max(0, absenceCount);
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.seatNumber = seatNumber;
    }

    // Checks if the student has warning-level absences.
    boolean isWarning() {
        return absenceCount == 2;
    }

    // Checks if the student has FDA-level absences.
    boolean isFda() {
        return absenceCount >= 3;
    }

    // Trims text and safely turns null into an empty string.
    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
