package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class RegularClassroom extends ScaledPanel {

    // if the classroom is too big and needs scaling, use s(176) for the total width
    // otherwise, use the actual width of the classroom

    private static final int MAX_COLUMNS = 14;
    private static final int COLUMN_SPACING = 4;
    private static final int MAX_ROWS = 4;

    public RegularClassroom(double scale, Classroom room) {
        super(scale);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(s(10), s(10), s(10), s(10)));
        setOpaque(false);

        add(Box.createVerticalGlue());
        add(buildTeacherFrontPanel(scale, room));
        add(buildSeatsPanel(scale, room));
        add(Box.createVerticalGlue());
    }

    private ScaledPanel buildTeacherFrontPanel(double scale, Classroom room) {
        int seatSize = s(70);
        ScaledPanel teacherFrontPanel = new ScaledPanel(scale);
        teacherFrontPanel.setOpaque(false);
        teacherFrontPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(120)));
        teacherFrontPanel.setLayout(new BoxLayout(teacherFrontPanel, BoxLayout.Y_AXIS));

        String tName = room.getTeacherName() != null ? room.getTeacherName() : "Empty";
        Seat teacherSeat = new Seat(scale, 0, tName, false, Seat.SeatType.TEACHER);
        teacherSeat.setPreferredSize(new Dimension(seatSize, seatSize));
        teacherSeat.setMaximumSize(new Dimension(seatSize, seatSize));
        teacherSeat.setAlignmentX(Component.CENTER_ALIGNMENT);
        room.addSeat(teacherSeat);

        Table teacherTable = new Table(scale, 0, s(200), s(60));
        teacherTable.setAlignmentX(Component.CENTER_ALIGNMENT);

        teacherFrontPanel.add(Box.createVerticalStrut(s(5)));
        teacherFrontPanel.add(teacherSeat);
        teacherFrontPanel.add(Box.createVerticalStrut(s(5)));
        teacherFrontPanel.add(teacherTable);
        teacherFrontPanel.add(Box.createVerticalStrut(s(5)));

        return teacherFrontPanel;
    }

    private ScaledPanel buildSeatsPanel(double scale, Classroom room) {
        int seatSize = s(70);
        ScaledPanel seatsRowPanel = new ScaledPanel(scale);
        seatsRowPanel.setLayout(new BoxLayout(seatsRowPanel, BoxLayout.Y_AXIS));
        seatsRowPanel.setOpaque(false);
        seatsRowPanel.setBorder(new EmptyBorder(s(10), s(10), s(10), s(10)));

        for (int row = 0; row < MAX_ROWS - 1; row++) {
            JPanel rowPanel = new JPanel(new GridLayout(1, MAX_COLUMNS, s(5), s(5)));
            rowPanel.setOpaque(false);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, seatSize));
            int seatsInRow = 0;
            for (int col = 0; col < MAX_COLUMNS; col++) {
                if (seatsInRow > 0 && seatsInRow % COLUMN_SPACING == 0) {
                    JPanel spacer = new JPanel();
                    spacer.setOpaque(false);
                    rowPanel.add(spacer);
                    col++;
                }
                if (col >= MAX_COLUMNS)
                    break;
                Seat seat = Seat.createSeat(scale, Seat.SeatType.STUDENT);
                seat.setPreferredSize(new Dimension(seatSize, seatSize));
                room.addSeat(seat);
                rowPanel.add(seat);
                seatsInRow++;
            }
            seatsRowPanel.add(rowPanel);
            seatsRowPanel.add(Box.createVerticalStrut(s(3)));
        }

        JPanel lastRow = new JPanel(new GridLayout(1, MAX_COLUMNS, s(5), s(5)));
        lastRow.setOpaque(false);
        lastRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, seatSize));
        for (int col = 0; col < MAX_COLUMNS; col++) {
            Seat seat = Seat.createSeat(scale, Seat.SeatType.STUDENT);
            seat.setPreferredSize(new Dimension(seatSize, seatSize));
            room.addSeat(seat);
            lastRow.add(seat);
        }
        seatsRowPanel.add(lastRow);

        return seatsRowPanel;
    }
}