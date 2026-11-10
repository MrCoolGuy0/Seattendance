package com.seatendance;

import javax.swing.*;

public class ScaledPanel extends JPanel {

    protected final double scale;

    public ScaledPanel(double scale) {
        this.scale = scale;
    }

    protected int s(double base) {
        return (int) (base * scale);
    }

}