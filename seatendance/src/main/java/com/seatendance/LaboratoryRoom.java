package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class LaboratoryRoom extends ScaledPanel {

    // if the classroom is too big and needs scaling, use s(176) for the total width
    // otherwise, use the actual width of the classroom

    private static final int ROWS = 5;
    private static final int SEATS_PER_TABLE = 10;
    private static final int SEATS_PER_WALL = 5;

    public LaboratoryRoom(double scale, Classroom room) {
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

    private ScaledPanel buildSeatandContainer(double scale, Classroom room, int seatSize) {
        ScaledPanel containerPanel = new ScaledPanel(scale);
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        containerPanel.setOpaque(false);
        containerPanel.setBorder(new EmptyBorder(0, s(5), 0, s(5)));

        for (int i = 0; i < ROWS; i++) {

            containerPanel.add(createRowOfSeats(scale, (int) Math.floor(SEATS_PER_TABLE / 2), seatSize, room));
            containerPanel.add(Box.createVerticalStrut(s(5)));

            Table table = Table.createTable(scale, Integer.MAX_VALUE, seatSize);
            containerPanel.add(table);
            containerPanel.add(Box.createVerticalStrut(s(5)));

            containerPanel.add(createRowOfSeats(scale, (int) Math.floor(SEATS_PER_TABLE / 2), seatSize, room));
            containerPanel.add(Box.createVerticalStrut(s(20)));

        }

        return containerPanel;
    }

    private ScaledPanel buildSeatsPanel(double scale, Classroom room) {
        int seatSize = s(70);

        ScaledPanel classroomLayoutPanel = new ScaledPanel(scale);
        classroomLayoutPanel.setLayout(new BoxLayout(classroomLayoutPanel, BoxLayout.Y_AXIS));
        classroomLayoutPanel.setOpaque(false);
        classroomLayoutPanel.setBorder(new EmptyBorder(s(10), s(10), s(10), s(10)));

        ScaledPanel seatAndTables = new ScaledPanel(scale);
        seatAndTables.setLayout(new BorderLayout());
        seatAndTables.setOpaque(false);
        seatAndTables.add(buildSeatandContainer(scale, room, seatSize), BorderLayout.CENTER);
        seatAndTables.add(createColumnOfSeats(scale, SEATS_PER_WALL, seatSize, room), BorderLayout.WEST);
        seatAndTables.add(createColumnOfSeats(scale, SEATS_PER_WALL, seatSize, room), BorderLayout.EAST);

        classroomLayoutPanel.add(seatAndTables);

        return classroomLayoutPanel;
    }

    private ScaledPanel createColumnOfSeats(double scale, int seatsPerWall, int seatSize, Classroom room) {
        int height = seatsPerWall * seatSize + (seatsPerWall - 1) * s(5);

        ScaledPanel columnPanel = new ScaledPanel(scale);
        columnPanel.setLayout(new GridLayout(seatsPerWall, 1, 0, s(5)));
        columnPanel.setOpaque(false);
        columnPanel.setMinimumSize(new Dimension(seatSize, height));
        columnPanel.setMaximumSize(new Dimension(seatSize, height));
        // the columnPanel width is 60

        for (int i = 0; i < seatsPerWall; i++) {
            ScaledPanel seatContainer = new ScaledPanel(scale);
            seatContainer.setLayout(new GridBagLayout());
            seatContainer.setBorder(new EmptyBorder(2, 2, 2, 2));
            seatContainer.setOpaque(false);

            Seat seat = Seat.createSeat(scale, Seat.SeatType.STUDENT);
            seat.setPreferredSize(new Dimension(seatSize, seatSize));
            seat.setMaximumSize(new Dimension(seatSize, seatSize));

            seatContainer.add(seat);
            room.addSeat(seat);

            columnPanel.add(seatContainer);
        }

        return columnPanel;
    }

    private ScaledPanel createRowOfSeats(double scale, int seatsPerRow, int seatSize, Classroom room) {
        ScaledPanel rowPanel = new ScaledPanel(scale);
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.setOpaque(false);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, seatSize));
        rowPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, seatSize));

        for (int i = 0; i < seatsPerRow; i++) {
            Seat seat = Seat.createSeat(scale, Seat.SeatType.STUDENT);
            seat.setPreferredSize(new Dimension(seatSize, seatSize));
            seat.setMaximumSize(new Dimension(seatSize, seatSize));
            room.addSeat(seat);

            rowPanel.add(Box.createHorizontalGlue());
            rowPanel.add(seat);

            if (i == seatsPerRow - 1) {
                rowPanel.add(Box.createHorizontalGlue());
            }
        }

        return rowPanel;
    }
}