package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;

public class ClassroomBuilder extends ScaledPanel {

    private DatabaseHandler db;
    private double scale;
    private MainSeatendanceApp app;

    private static final Color CONTENT_BG = new Color(34, 38, 42);
    private static final Color CARD_BG = new Color(20, 25, 28);
    private static final Color PANEL_BG = new Color(22, 28, 32);
    private static final Color CONTROL_BG = new Color(25, 31, 36);
    private static final Color BORDER = new Color(63, 70, 78);
    private static final Color TEXT = Color.WHITE;
    private static final Color MUTED_TEXT = new Color(174, 182, 193);
    private static final Color META = new Color(137, 148, 160);
    private static final Color STAT_GREEN = new Color(43, 83, 63);
    private static final Color STAT_RED = new Color(111, 34, 36);
    private static final Color STAT_GOLD = new Color(113, 76, 28);
    private static final Color STAT_BLUE = new Color(28, 72, 113);

    public JPanel classroomPanel;
    private JPanel statsRow;

    private String currentSession;
    private boolean isSessionStarted;
    private String lockedClassroomType;
    private String teacherName;

    public ClassroomBuilder(double scale, DatabaseHandler db, String currentSession,
            boolean isSessionStarted, String lockedClassroomType, String teacherName) {
        super(scale);
        this.scale = scale;
        this.db = db;
        this.app = null; // will be set via setApp() after construction
        setOpaque(true);
        setBackground(CONTENT_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(s(32), s(30), s(36), s(28)));

        this.currentSession = currentSession;
        this.isSessionStarted = isSessionStarted;
        this.lockedClassroomType = lockedClassroomType;
        this.teacherName = teacherName;

        String initialType = (isSessionStarted && lockedClassroomType != null)
                ? lockedClassroomType
                : "Computer Laboratory";

        java.util.Map<Integer, String> occupancy = null;
        if (currentSession != null) {
            try {
                String[] parts = currentSession.split("_");
                int sectionSubjectId = Integer.parseInt(parts[0]);
                occupancy = db.getSeatOccupants(sectionSubjectId, initialType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Classroom Builder");
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, s(32)));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(isSessionStarted
                ? "Viewing classroom for active session: " + lockedClassroomType
                : "Manage classroom seating and availability.");
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, s(20)));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        classroomPanel = new JPanel(new BorderLayout());
        classroomPanel.setOpaque(true);
        classroomPanel.setBackground(CARD_BG);

        // initialType already defined above
        classroomPanel.add(new Classroom(scale, initialType, occupancy, teacherName), BorderLayout.CENTER);

        statsRow = createStatsRow(occupancy != null ? occupancy.size() : 0);

        header.add(title);
        header.add(Box.createVerticalStrut(s(5)));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(s(22)));
        header.add(buildClassroomSelector(isSessionStarted, lockedClassroomType, initialType));
        header.add(Box.createVerticalStrut(s(14)));

        // Reset Seats button — only visible during an active session
        if (isSessionStarted) {
            JPanel resetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            resetRow.setOpaque(false);
            resetRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton resetBtn = new JButton("Reset Seat Arrangement");
            resetBtn.setFont(new Font("Segoe UI", Font.BOLD, s(13)));
            resetBtn.setBackground(new Color(100, 30, 30));
            resetBtn.setForeground(new Color(255, 110, 110));
            resetBtn.setFocusPainted(false);
            resetBtn.setBorderPainted(false);
            resetBtn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(160, 50, 50), 1),
                    new EmptyBorder(s(5), s(14), s(5), s(14))));
            resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            resetBtn.setToolTipText("Clear all seat assignments for this session");

            resetBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Reset all seat assignments for this session?\n" +
                                "Students will be re-assigned seats when they tap again.",
                        "Reset Seat Arrangement",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION)
                    return;

                boolean ok = (app != null && app.getSeatendance() != null)
                        ? app.getSeatendance().resetSeats()
                        : false;

                if (ok) {
                    refreshClassroom();
                    JOptionPane.showMessageDialog(this,
                            "Seat arrangement has been reset. Students will be re-assigned on next tap.",
                            "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Could not reset seats. Make sure a session is active.",
                            "Reset Failed", JOptionPane.ERROR_MESSAGE);
                }
            });

            resetRow.add(resetBtn);
            header.add(resetRow);
            header.add(Box.createVerticalStrut(s(14)));
        } else {
            header.add(Box.createVerticalStrut(s(8)));
        }

        header.add(statsRow);

        // Wrap classroomPanel in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(classroomPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(s(16));
        scrollPane.getHorizontalScrollBar().setUnitIncrement(s(16));

        // Style scrollbars to match the dark theme
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(63, 70, 78);
                trackColor = new Color(22, 28, 32);
            }

            @Override
            protected JButton createDecreaseButton(int o) {
                return makeZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int o) {
                return makeZeroButton();
            }

            private JButton makeZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });
        scrollPane.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(63, 70, 78);
                trackColor = new Color(22, 28, 32);
            }

            @Override
            protected JButton createDecreaseButton(int o) {
                return makeZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int o) {
                return makeZeroButton();
            }

            private JButton makeZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(s(24), 0, 0, 0));
        body.add(scrollPane, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void setApp(MainSeatendanceApp app) {
        this.app = app;
    }

    public ClassroomBuilder(double scale, DatabaseHandler db, String currentSession) {
        this(scale, db, currentSession, false, "Computer Laboratory", null);
    }

    private JPanel buildClassroomSelector(boolean isSessionStarted, String lockedClassroomType,
            String selectedType) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, s(10), 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(isSessionStarted
                ? "Classroom Type (locked to active session):"
                : "View Classroom Type:");
        label.setForeground(isSessionStarted ? META : MUTED_TEXT);
        label.setFont(new Font("Segoe UI", Font.PLAIN, s(15)));

        String[] types = { "Computer Laboratory", "Laboratory", "Regular Classroom" };

        ButtonGroup group = new ButtonGroup();
        for (String type : types) {
            JToggleButton btn = new JToggleButton(type) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean selected = isSelected();
                    boolean enabled = isEnabled();

                    if (selected) {
                        g2.setColor(isSessionStarted
                                ? new Color(45, 65, 90)
                                : new Color(60, 100, 160));
                    } else {
                        g2.setColor(enabled ? CONTROL_BG : new Color(22, 26, 30));
                    }
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(8), s(8));

                    if (selected) {
                        g2.setColor(isSessionStarted
                                ? new Color(60, 90, 120)
                                : new Color(80, 130, 200));
                    } else {
                        g2.setColor(enabled ? BORDER : new Color(45, 50, 56));
                    }
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, s(8), s(8));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Segoe UI", Font.PLAIN, s(14)));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setSelected(type.equals(selectedType));

            if (isSessionStarted) {
                btn.setEnabled(false);
                btn.setForeground(type.equals(selectedType) ? TEXT : META);
                btn.setCursor(Cursor.getDefaultCursor());
                btn.setToolTipText("Classroom type is locked to the active session");
            } else {
                btn.setEnabled(true);
                btn.setForeground(TEXT);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.setToolTipText(null);

                btn.addActionListener(e -> {
                    // Load occupants for the selected classroom type
                    java.util.Map<Integer, String> typeOccupancy = null;
                    if (currentSession != null) {
                        try {
                            String[] parts = currentSession.split("_");
                            int ssId = Integer.parseInt(parts[0]);
                            typeOccupancy = db.getSeatOccupants(ssId, type);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    classroomPanel.removeAll();
                    classroomPanel.add(new Classroom(scale, type, typeOccupancy, teacherName), BorderLayout.CENTER);
                    classroomPanel.revalidate();
                    classroomPanel.repaint();
                    refreshStatsRow(typeOccupancy != null ? typeOccupancy.size() : 0);
                });
            }

            Dimension btnSize = new Dimension(s(200), s(38));
            btn.setPreferredSize(btnSize);
            btn.setMinimumSize(btnSize);
            group.add(btn);
            row.add(btn);
        }

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel labelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelRow.setOpaque(false);
        labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelRow.add(label);

        if (isSessionStarted) {
            JLabel lockNote = new JLabel("Unlock by concluding the session in Start Session.");
            lockNote.setForeground(META);
            lockNote.setFont(new Font("Segoe UI", Font.PLAIN, s(12)));
            lockNote.setAlignmentX(Component.LEFT_ALIGNMENT);

            wrapper.add(labelRow);
            wrapper.add(Box.createVerticalStrut(s(8)));
            wrapper.add(row);
            wrapper.add(Box.createVerticalStrut(s(6)));

            JPanel lockRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            lockRow.setOpaque(false);
            lockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            lockRow.add(lockNote);
            wrapper.add(lockRow);
        } else {
            wrapper.add(labelRow);
            wrapper.add(Box.createVerticalStrut(s(8)));
            wrapper.add(row);
        }

        return wrapper;
    }

    private void refreshStatsRow() {
        JPanel parent = (JPanel) statsRow.getParent();
        int index = -1;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == statsRow) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            parent.remove(statsRow);
            statsRow = createStatsRow(0);
            parent.add(statsRow, index);
            parent.revalidate();
            parent.repaint();
        }
    }

    private JPanel createStatsRow(int takenCount) {
        JPanel row = new JPanel(new GridLayout(1, 4, s(22), 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(136)));
        row.setPreferredSize(new Dimension(0, s(136)));

        int total = Classroom.numberOfSeats;
        int taken = takenCount;
        int reserved = 0;
        int available = total - taken - reserved;

        row.add(createStatCard("Total Chairs", String.valueOf(total), STAT_BLUE));
        row.add(createStatCard("Available Chairs", String.valueOf(available), STAT_GREEN));
        row.add(createStatCard("Taken Chairs", String.valueOf(taken), STAT_RED));
        row.add(createStatCard("Reserved Chairs", String.valueOf(reserved), STAT_GOLD));

        return row;
    }

    private JPanel createStatCard(String title, String value, Color iconBg) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(10), s(10));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, s(10), s(10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(s(18), s(16), s(16), s(18)));

        GridBagConstraints gbc = new GridBagConstraints();

        JPanel circle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(iconBg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        circle.setOpaque(false);
        Dimension circleSize = new Dimension(s(62), s(62));
        circle.setPreferredSize(circleSize);
        circle.setMinimumSize(circleSize);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(0, 0, 0, s(14));
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(circle, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, s(5), 0);
        gbc.anchor = GridBagConstraints.WEST;
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, s(18)));
        card.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, s(48)));
        card.add(valueLabel, gbc);

        return card;
    }

    public void refreshClassroom() {
        if (classroomPanel.getComponentCount() > 0 && classroomPanel.getComponent(0) instanceof Classroom) {
            Classroom current = (Classroom) classroomPanel.getComponent(0);
            java.util.Map<Integer, String> occupancy = null;
            if (currentSession != null) {
                try {
                    String[] parts = currentSession.split("_");
                    int sectionSubjectId = Integer.parseInt(parts[0]);
                    String cType = current.getClassroomType();
                    occupancy = db.getSeatOccupants(sectionSubjectId, cType);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (Seat seat : current.getAllSeats()) {
                if (seat.getSeatType() == Seat.SeatType.STUDENT) {
                    seat.vacate();
                }
            }
            if (occupancy != null) {
                for (java.util.Map.Entry<Integer, String> entry : occupancy.entrySet()) {
                    current.occupySeat(entry.getKey(), entry.getValue());
                }
            }
            refreshStatsRow(occupancy != null ? occupancy.size() : 0);
        }
    }

    private void refreshStatsRow(int takenCount) {
        JPanel parent = (JPanel) statsRow.getParent();
        int index = -1;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == statsRow) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            parent.remove(statsRow);
            statsRow = createStatsRow(takenCount);
            parent.add(statsRow, index);
            parent.revalidate();
            parent.repaint();
        }
    }
}