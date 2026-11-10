package com.seatendance;

import javax.swing.*;

public abstract class ScaledWindow extends JWindow {

    protected final double scale;

    public ScaledWindow(double scale) {
        this.scale = scale;
    }

    protected int s(int base) {
        return (int) (base * scale);
    }

}