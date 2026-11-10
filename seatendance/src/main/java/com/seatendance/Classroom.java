package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Classroom extends ScaledPanel {

    public static int numberOfSeats = 0;
    public static int numberOfTables = 0;

    private List<Seat> seats = new ArrayList<>();

    public enum RoomType {
        COMPUTER_LAB,
        REGULAR,
        LABORATORY;

        public static RoomType fromString(String type) {

            if (type == null) {
                return REGULAR;
            }

            switch (type.trim()) {

                case "Computer Laboratory": {
                    return COMPUTER_LAB;
                }

                case "Laboratory": {
                    return LABORATORY;
                }

                default: {
                    return REGULAR;
                }

            }
        }
    }

    private final RoomType roomType;
    private final double scale;
    private final String teacherName;

    public Classroom(double scale, RoomType roomType, java.util.Map<Integer, String> occupancy, String teacherName) {
        super(scale);
        this.scale = scale;
        this.roomType = roomType;
        this.teacherName = teacherName;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(buildRoom(), BorderLayout.CENTER);

        if (occupancy != null) {
            for (java.util.Map.Entry<Integer, String> entry : occupancy.entrySet()) {
                occupySeat(entry.getKey(), entry.getValue());
            }
        }
    }

    public Classroom(double scale, RoomType roomType) {
        this(scale, roomType, null, null);
    }

    public Classroom(double scale, String type) {
        this(scale, RoomType.fromString(type), null, null);
    }

    public Classroom(double scale, String type, java.util.Map<Integer, String> occupancy) {
        this(scale, RoomType.fromString(type), occupancy, null);
    }

    public Classroom(double scale, String type, java.util.Map<Integer, String> occupancy, String teacherName) {
        this(scale, RoomType.fromString(type), occupancy, teacherName);
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getClassroomType() {
        switch (roomType) {
            case COMPUTER_LAB:
                return "Computer Laboratory";
            case LABORATORY:
                return "Laboratory";
            case REGULAR:
                return "Regular Classroom";
            default:
                return "Regular Classroom";
        }
    }

    private ScaledPanel buildRoom() {
        switch (roomType) {

            case REGULAR: {
                resetCounts();
                return new RegularClassroom(scale, this);
            }

            case LABORATORY: {
                resetCounts();
                return new LaboratoryRoom(scale, this);
            }

            case COMPUTER_LAB: {
                resetCounts();
                return new ComputerLaboratory(scale, this);
            }

            default: {
                return new RegularClassroom(scale, this);
            }

        }
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public Seat getSeat(int seatNumber) {
        for (Seat seat : seats) {
            if (seat.getSeatNumber() == seatNumber)
                return seat;
        }
        return null;
    }

    public void vacateSeat(int seatNumber) {
        Seat seat = getSeat(seatNumber);
        if (seat == null)
            return;
        seat.vacate();
    }

    public void occupySeat(int seatNumber, String occupant) {
        Seat seat = getSeat(seatNumber);
        if (seat == null)
            return;
        seat.occupy(occupant);
    }

    public void toggleSeatReservation(int seatNumber) {
        Seat seat = getSeat(seatNumber);
        if (seat == null)
            return;
        seat.toggleReserve();
    }

    public List<Seat> getAllSeats() {
        return seats;
    }

    public void occupyRandomly(String occupant) {
        List<Seat> availableSeats = new ArrayList<>();
        for (Seat seat : seats) {
            if (!seat.isSeatTaken() && seat.getSeatType() == Seat.SeatType.STUDENT) {
                availableSeats.add(seat);
            }
        }
        if (availableSeats.isEmpty())
            return;
        int randomSeat = (int) (Math.random() * availableSeats.size());
        availableSeats.get(randomSeat).occupy(occupant);
    }

    public static void resetCounts() {
        numberOfSeats = 0;
        numberOfTables = 0;
    }
}

class Seat extends ScaledPanel {
    private int seatNumber;
    private boolean isSeatTaken = false;
    private boolean isReserved = false;
    private String currentOccupant;
    private JLabel seatLabel;
    private JLabel seatOccupantLabel;

    public enum SeatType {
        STUDENT,
        TEACHER
    }

    private SeatType seatType;

    private JLabel makeSeatLabel() {
        if (seatType == SeatType.TEACHER) {
            seatLabel = new JLabel("Teacher");
        } else {
            seatLabel = new JLabel(String.valueOf(seatNumber));
        }
        seatLabel.setFont(new Font("Arial", Font.BOLD, (int) s(10)));
        seatLabel.setHorizontalAlignment(SwingConstants.CENTER);
        seatLabel.setVerticalAlignment(SwingConstants.CENTER);
        seatLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        seatLabel.setForeground(Color.WHITE);
        return seatLabel;
    }

    private JLabel makeSeatOccupantLabel() {
        String defaultOccupant = (seatType == SeatType.TEACHER) ? "Empty" : "Empty";
        if (seatType == SeatType.TEACHER && currentOccupant != null && !currentOccupant.isEmpty()) {
            defaultOccupant = currentOccupant;
        }
        seatOccupantLabel = new JLabel(isSeatTaken ? currentOccupant : defaultOccupant);

        seatOccupantLabel.setFont(new Font("Arial", Font.BOLD, (int) s(10)));
        seatOccupantLabel.setHorizontalAlignment(SwingConstants.CENTER);
        seatOccupantLabel.setVerticalAlignment(SwingConstants.CENTER);
        seatOccupantLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        seatOccupantLabel.setForeground(Color.WHITE);
        return seatOccupantLabel;
    }

    public Seat(double scale, int seatNumber, String currentOccupant, boolean isSeatTaken, SeatType seatType) {
        super(scale);
        this.seatNumber = seatNumber;
        this.currentOccupant = currentOccupant;
        this.isSeatTaken = isSeatTaken;
        this.seatType = seatType;

        setMaximumSize(new Dimension(70, 70));
        setOpaque(false);

        setLayout(new GridLayout(2, 1));
        add(makeSeatLabel());
        add(makeSeatOccupantLabel());
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        super.paintComponent(g2d);

        int w = getWidth();
        int h = getHeight();

        g2d.setColor(new Color(30, 30, 30));
        g2d.setStroke(new BasicStroke(3));
        g2d.fillRoundRect(1, 1, w - 2, h - 2, 20, 20);

        Color borderColor;
        if (isReserved) {
            borderColor = Color.decode("#B45309");
        } else if (isSeatTaken) {
            borderColor = Color.decode("#EF4444");
        } else {
            borderColor = Color.decode("#22C55E");
        }

        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(1, 1, w - 2, h - 2, 20, 20);
    }

    public static Seat createSeat(double scale, SeatType seatType) {
        Classroom.numberOfSeats++;
        return new Seat(scale, Classroom.numberOfSeats, "Empty", false, seatType);
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public boolean isSeatTaken() {
        return isSeatTaken;
    }

    public String getCurrentOccupant() {
        return currentOccupant;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void occupy(String occupant) {
        this.isSeatTaken = true;
        this.isReserved = false;
        this.currentOccupant = occupant;
        seatOccupantLabel.setText(occupant);
        this.repaint();
    }

    public void vacate() {
        this.isSeatTaken = false;
        this.isReserved = false;
        this.currentOccupant = null;
        seatOccupantLabel.setText("Empty");
        this.repaint();
    }

    public void toggleReserve() {
        if (isSeatTaken && !isReserved) {
            return; // don't reserve an occupied seat
        }

        this.isReserved = !isReserved;
        this.isSeatTaken = isReserved;
        seatOccupantLabel.setText(isReserved ? "Reserved" : "Empty");
        this.repaint();
    }
}

class Table extends ScaledPanel {
    private int tableNumber;
    private int tableWidth;
    private int tableHeight;

    private JLabel tableLabel;

    private JLabel makeTableLabel() {
        if (tableNumber == 0) {
            tableLabel = new JLabel("Teacher's Table");
        } else {
            tableLabel = new JLabel("Table " + String.valueOf(tableNumber));
        }
        tableLabel.setFont(new Font("Arial", Font.BOLD, (int) s(10)));
        tableLabel.setHorizontalAlignment(SwingConstants.CENTER);
        tableLabel.setVerticalAlignment(SwingConstants.CENTER);
        tableLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tableLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        tableLabel.setForeground(Color.WHITE);
        return tableLabel;
    }

    public Table(double scale, int tableNumber, int width, int height) {
        super(scale);
        this.tableNumber = tableNumber;
        this.tableWidth = width;
        this.tableHeight = height;

        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setOpaque(false);

        add(Box.createVerticalGlue());
        add(makeTableLabel());
        add(Box.createVerticalGlue());
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        super.paintComponent(g2d);

        int w = getWidth();
        int h = getHeight();

        g2d.setColor(new Color(30, 30, 30));
        g2d.setStroke(new BasicStroke(3));
        g2d.fillRoundRect(1, 1, w - 2, h - 2, 20, 20);

        g2d.setColor(new Color(253, 252, 250));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(1, 1, w - 2, h - 2, 20, 20);

    }

    public static Table createTable(double scale, int width, int height) {
        Classroom.numberOfTables++;
        return new Table(scale, Classroom.numberOfTables, width, height);
    }

    public int getTableNumber() {
        return tableNumber;
    }
}