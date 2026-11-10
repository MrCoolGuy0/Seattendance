package com.seatendance;

import java.awt.*;

public class MainSeatendanceApp {

    private final double scale;
    private final DatabaseHandler db = new DatabaseHandler();
    private RFIDManager rfidManager;

    public MainSeatendanceApp() {
        scale = computeScale();
        showSplash();
    }

    private static double computeScale() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        return Math.min((double) screen.width / 1920, (double) screen.height / 1080);
    }

    public void showSplash() {
        new SeatendanceSplash(this, scale);
    }

    public void showLogin() {
        new LoginFrame(this, scale);
    }

    private Seatendance currentSeatendance;

    public void showSeatendance(String email, String role) {
        currentSeatendance = new Seatendance(this, scale, email, role);
    }

    public Seatendance getSeatendance() {
        return currentSeatendance;
    }

    public Seatendance getMainFrame() {
        return currentSeatendance;
    }

    public void startRFID() {
        rfidManager = new RFIDManager();
        rfidManager.start();
    }

    public RFIDManager getRFIDManager() {
        return rfidManager;
    }

    public DatabaseHandler getDatabase() {
        return db;
    }

    public RFIDScanner getRFIDScanner() {
        return rfidManager.getScanner();
    }

}