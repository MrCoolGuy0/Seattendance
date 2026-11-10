package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class ComputerLaboratory extends ScaledPanel {

    // if the classroom is too big and needs scaling, use s(176) for the total width
    // otherwise, use the actual width of the classroom

    private final static int SEATS_PER_COLUMN = 7;
    private final static int SEATS_PER_ROW = 8;

    public ComputerLaboratory(double scale, Classroom room) {
        super(scale);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(s(10), s(10), s(10), s(10)));
        setOpaque(false);
        setPreferredSize(new Dimension(s(176), s(814)));
        // 524 + 145 + 145 = 814
        // Vertical Tables Container + Horizontal Tables Container + Teachers Panel

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
        teacherFrontPanel.setPreferredSize(new Dimension(0, s(145)));

        String tName = room.getTeacherName() != null ? room.getTeacherName() : "Empty";
        Seat teacherSeat = new Seat(scale, 0, tName, false, Seat.SeatType.TEACHER);
        teacherSeat.setPreferredSize(new Dimension(seatSize, seatSize));
        teacherSeat.setMaximumSize(new Dimension(seatSize, seatSize));
        teacherSeat.setAlignmentX(Component.CENTER_ALIGNMENT);
        room.addSeat(teacherSeat);

        Table teacherTable = new Table(scale, 0, s(200), s(60));

        teacherFrontPanel.add(Box.createVerticalStrut(s(5)));
        teacherFrontPanel.add(teacherSeat);
        teacherFrontPanel.add(Box.createVerticalStrut(s(5)));
        teacherFrontPanel.add(teacherTable);
        teacherFrontPanel.add(Box.createVerticalStrut(s(5)));

        return teacherFrontPanel;
    }

    private ScaledPanel buildSeatsPanel(double scale, Classroom room) {
        int seatSize = s(70);

        ScaledPanel classroomLayoutPanel = new ScaledPanel(scale);
        classroomLayoutPanel.setLayout(new BoxLayout(classroomLayoutPanel, BoxLayout.Y_AXIS));
        classroomLayoutPanel.setOpaque(false);
        classroomLayoutPanel.setBorder(new EmptyBorder(s(10), s(10), s(10), s(10)));

        ScaledPanel verticalTablesContainer = new ScaledPanel(scale);
        verticalTablesContainer.setLayout(new BoxLayout(verticalTablesContainer, BoxLayout.X_AXIS));
        verticalTablesContainer.setOpaque(false);

        int tableHeight = (seatSize * SEATS_PER_COLUMN) + (s(5) * SEATS_PER_COLUMN - 1);

        Table table1 = Table.createTable(scale, seatSize, tableHeight);
        Table table2 = Table.createTable(scale, seatSize, tableHeight);
        Table table3 = Table.createTable(scale, seatSize, tableHeight);
        Table table4 = Table.createTable(scale, seatSize, tableHeight);

        ScaledPanel seatsPanel1 = createColumnOfSeats(scale, SEATS_PER_COLUMN, seatSize, room);
        ScaledPanel seatsPanel2 = createColumnOfSeats(scale, SEATS_PER_COLUMN, seatSize, room);
        ScaledPanel seatsPanel3 = createColumnOfSeats(scale, SEATS_PER_COLUMN, seatSize, room);
        ScaledPanel seatsPanel4 = createColumnOfSeats(scale, SEATS_PER_COLUMN, seatSize, room);
        ScaledPanel seatsPanel5 = createColumnOfSeats(scale, SEATS_PER_COLUMN, seatSize, room);
        ScaledPanel seatsPanel6 = createColumnOfSeats(scale, SEATS_PER_COLUMN, seatSize, room);

        verticalTablesContainer.add(table1);
        verticalTablesContainer.add(Box.createHorizontalStrut(s(5)));
        verticalTablesContainer.add(seatsPanel1);
        verticalTablesContainer.add(Box.createHorizontalGlue());
        verticalTablesContainer.add(seatsPanel2);
        verticalTablesContainer.add(Box.createHorizontalStrut(s(5)));
        verticalTablesContainer.add(table2);
        verticalTablesContainer.add(Box.createHorizontalStrut(s(5)));
        verticalTablesContainer.add(seatsPanel3);
        verticalTablesContainer.add(Box.createHorizontalGlue());
        verticalTablesContainer.add(seatsPanel4);
        verticalTablesContainer.add(Box.createHorizontalStrut(s(5)));
        verticalTablesContainer.add(table3);
        verticalTablesContainer.add(Box.createHorizontalStrut(s(5)));
        verticalTablesContainer.add(seatsPanel5);
        verticalTablesContainer.add(Box.createHorizontalGlue());
        verticalTablesContainer.add(seatsPanel6);
        verticalTablesContainer.add(Box.createHorizontalStrut(s(5)));
        verticalTablesContainer.add(table4);

        ScaledPanel horizontalTablesContainer = new ScaledPanel(scale);
        horizontalTablesContainer.setLayout(new BoxLayout(horizontalTablesContainer, BoxLayout.Y_AXIS));
        horizontalTablesContainer.setOpaque(false);

        ScaledPanel seatsPanel7 = createRowOfSeats(scale, SEATS_PER_ROW, seatSize, room);
        Table table5 = Table.createTable(scale, Integer.MAX_VALUE, seatSize);

        horizontalTablesContainer.add(seatsPanel7);
        horizontalTablesContainer.add(Box.createVerticalStrut(s(5)));
        horizontalTablesContainer.add(table5);

        classroomLayoutPanel.add(verticalTablesContainer);
        classroomLayoutPanel.add(Box.createVerticalStrut(s(5)));
        classroomLayoutPanel.add(horizontalTablesContainer);

        return classroomLayoutPanel;
    }

    private ScaledPanel createColumnOfSeats(double scale, int numSeats, int seatSize, Classroom room) {
        // Height is 520
        int height = numSeats * seatSize + (numSeats - 1) * s(5);

        ScaledPanel columnPanel = new ScaledPanel(scale);
        columnPanel.setLayout(new GridLayout(numSeats, 1, 0, s(5)));
        columnPanel.setOpaque(false);
        columnPanel.setMaximumSize(new Dimension(seatSize, height));
        columnPanel.setPreferredSize(new Dimension(seatSize, height));

        for (int i = 0; i < numSeats; i++) {
            Seat seat = Seat.createSeat(scale, Seat.SeatType.STUDENT);
            room.addSeat(seat);
            columnPanel.add(seat);
        }

        return columnPanel;
    }

    private ScaledPanel createRowOfSeats(double scale, int numSeats, int seatSize, Classroom room) {
        ScaledPanel rowPanel = new ScaledPanel(scale);
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.setOpaque(false);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, seatSize));
        rowPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, seatSize));

        for (int i = 0; i < numSeats; i++) {
            Seat seat = Seat.createSeat(scale, Seat.SeatType.STUDENT);
            seat.setPreferredSize(new Dimension(seatSize, seatSize));
            seat.setMaximumSize(new Dimension(seatSize, seatSize));
            room.addSeat(seat);

            rowPanel.add(Box.createHorizontalGlue());
            rowPanel.add(seat);
            if (i == numSeats - 1) {
                rowPanel.add(Box.createHorizontalGlue());
            }
        }

        return rowPanel;
    }
}