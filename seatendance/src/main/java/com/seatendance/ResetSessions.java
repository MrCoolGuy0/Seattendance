package com.seatendance;

import java.sql.*;
import java.nio.file.*;

/**
 * One-off utility to reset all attendance sessions, attendance records,
 * and seat assignments while keeping enrollment data intact.
 */
public class ResetSessions {
    public static void main(String[] args) {
        // Reuse the same DB path logic as DatabaseHandler
        Path modulePath = Paths.get("").toAbsolutePath();
        if (!Files.exists(modulePath.resolve("pom.xml"))) {
            modulePath = modulePath.resolve("seatendance");
        }
        String dbFile = modulePath.resolve("seatendance.db").toString().replace('\\', '/');
        String url = "jdbc:sqlite:" + dbFile;
        System.out.println("Connecting to: " + dbFile);

        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {
            int sessions = stmt.executeUpdate("DELETE FROM AttendanceSessions");
            System.out.println("Deleted " + sessions + " rows from AttendanceSessions");

            int attendance = stmt.executeUpdate("DELETE FROM Attendance");
            System.out.println("Deleted " + attendance + " rows from Attendance");

            int seats = stmt.executeUpdate("DELETE FROM SeatAssignments");
            System.out.println("Deleted " + seats + " rows from SeatAssignments");

            System.out.println("\nAll sessions reset! Students, teachers, and enrollments are untouched.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
