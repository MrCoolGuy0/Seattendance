package com.seatendance;

import java.awt.*;

public class AppSettings {

    public static class AppColors {
        static final Color white = Color.WHITE;
        static final Color black = Color.BLACK;
        static final Color darkGray = new Color(30, 30, 30);
        static final Color darkGray2 = new Color(42, 42, 42);
        static final Color gray = new Color(60, 60, 60);
        static final Color lightGray = new Color(90, 90, 90);

        // Accents
        static final Color maroon = new Color(128, 0, 0);
        static final Color blue = new Color(30, 100, 255);
        static final Color pink = new Color(255, 105, 180);
        static final Color yellow = new Color(255, 210, 0);
        static final Color violet = new Color(138, 43, 226);
        static final Color darkGreen = new Color(0, 100, 0);
        static final Color red = new Color(220, 50, 50);
        static final Color crimson = new Color(220, 20, 60);
        static final Color sageGreen = new Color(143, 188, 143);
        static final Color royalBlue = new Color(65, 105, 225);
    }

    public enum Program {
        // College
        COMPUTER_SCIENCE(AppColors.maroon),
        MEDICAL_TECHNOLOGY(AppColors.pink),
        PSYCHOLOGY(AppColors.blue),
        PHYSICAL_THERAPY(AppColors.red),
        PHARMACY(AppColors.violet),
        CRIMINOLOGY(AppColors.darkGreen),
        NURSING(AppColors.yellow),

        // Senior High School
        STEM(AppColors.sageGreen),
        HUMSS(AppColors.royalBlue),
        ABM(AppColors.crimson);

        public final Color accent;

        Program(Color accent) {
            this.accent = accent;
        }
    }

    public enum Theme {
        LIGHT(AppColors.white, AppColors.black),
        DARK(AppColors.darkGray, AppColors.white);

        public final Color background;
        public final Color text;

        Theme(Color background, Color text) {
            this.background = background;
            this.text = text;
        }

        public static Theme getTheme(String value) {
            try {
                return Theme.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return LIGHT;
            }
        }
    }

}