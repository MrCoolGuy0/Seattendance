package com.seatendance;

import javax.swing.*;

public abstract class ScaledFrame extends JFrame {

    protected final double scale;

    public ScaledFrame(double scale) {
        this.scale = scale;
    }

    protected int s(int base) {
        return (int) (base * scale);
    }

}