package com.seatendance;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;

// One clickable student card in the attendance grid.
final class StudentCard extends JButton {
    final StudentRecord student;
    private final StartSessionPanel panel;
    private final JLabel nameLabel;
    private final JLabel idLabel;
    private final StatusBadge statusBadge;
    private final TimeEntryField timeInField;
    private final TimeEntryField timeOutField;
    private final Timer hoverTimer;
    private boolean hovered;
    private boolean pressed;
    private double hoverProgress;

    // Builds one clickable student attendance card.
    StudentCard(StartSessionPanel panel, StudentRecord student) {
        this.panel = panel;
        this.student = student;
        // JButton is used so the whole card can be clicked like a button.
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(s(10), s(16), s(10), s(16)));
        setPreferredSize(new Dimension(s(330), s(156)));
        setMinimumSize(new Dimension(s(300), s(146)));

        // Timer makes the card hover animation smooth.
        hoverTimer = new Timer(15, e -> updateHoverAnimation());
        hoverTimer.setCoalesce(true);

        JPanel content = new JPanel() {
            // Slightly zooms the card content during hover.
            @Override
            protected void paintChildren(Graphics g) {
                // Makes the text/buttons slightly zoom when the card is hovered.
                double progress = contentHoverProgress();
                if (progress <= 0.0) {
                    super.paintChildren(g);
                    return;
                }

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.clipRect(0, 0, getWidth(), getHeight());
                double scale = 1.0 + 0.025 * progress;
                double centerX = getWidth() / 2.0;
                double centerY = getHeight() / 2.0;
                g2.translate(centerX, centerY - s(1) * progress);
                g2.scale(scale, scale);
                g2.translate(-centerX, -centerY);
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        nameLabel = new JLabel("", SwingConstants.CENTER);
        nameLabel.setForeground(StartSessionPanel.TEXT);
        nameLabel.setFont(panel.font(Font.BOLD, 15));
        nameLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(7), 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameRow.add(panel.profileIcon(18, 0.64f));
        nameRow.add(nameLabel);

        idLabel = new JLabel("", SwingConstants.CENTER);
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        idLabel.setForeground(StartSessionPanel.MUTED);
        idLabel.setFont(panel.font(Font.PLAIN, 13));

        statusBadge = new StatusBadge(student);
        statusBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel timeRow = new JPanel(new GridLayout(1, 2, s(10), 0));
        timeRow.setOpaque(false);
        timeRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(44)));

        timeInField = new TimeEntryField(student, true);
        timeRow.add(timeInField);

        timeOutField = new TimeEntryField(student, false);
        timeRow.add(timeOutField);

        content.add(Box.createVerticalGlue());
        content.add(nameRow);
        content.add(Box.createVerticalStrut(s(3)));
        content.add(idLabel);
        content.add(Box.createVerticalStrut(s(6)));
        content.add(timeRow);
        content.add(Box.createVerticalStrut(s(6)));
        content.add(statusBadge);
        content.add(Box.createVerticalGlue());
        add(content, BorderLayout.CENTER);

        addActionListener(e -> panel.cycleStudent(student));
        addMouseListener(new MouseAdapter() {
            // Starts the hover animation when the mouse enters the card.
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                hoverTimer.start();
            }

            // Stops the hover animation when the mouse leaves the card.
            @Override
            public void mouseExited(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    // Delay this check so moving over child fields does not cancel hover.
                    if (getMousePosition(true) == null) {
                        hovered = false;
                        hoverTimer.start();
                    }
                });
            }

            // Marks the card as pressed while the left mouse button is down.
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                    pressed = true;
                    repaint();
                }
            }

            // Clears the pressed state when the mouse button is released.
            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
        refresh();
    }

    // Updates the hover animation progress for the card.
    private void updateHoverAnimation() {
        // Moves hoverProgress slowly toward 0 or 1 for smooth animation.
        double target = 0.0;
        if (hovered && isEnabled()) {
            target = 1.0;
        }
        double step = 0.16;

        if (Math.abs(hoverProgress - target) <= step) {
            hoverProgress = target;
            hoverTimer.stop();
        } else if (hoverProgress < target) {
            hoverProgress += step;
        } else {
            hoverProgress -= step;
        }

        repaint();
    }

    // Returns the hover amount used for scaling card content.
    private double contentHoverProgress() {
        if (isEnabled() && !hasEditingFocus()) {
            return hoverProgress;
        }
        return 0.0;
    }

    // Checks if one of the card's child fields is being edited.
    private boolean hasEditingFocus() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, this);
    }

    // Refreshes the student card text, status, time fields, and enabled state.
    void refresh() {
        nameLabel.setText(panel.fitText(student.name, nameLabel.getFont(), textWidthForCard()));
        idLabel.setText(student.studentId);
        statusBadge.refresh();
        boolean timeEnabled = panel.selectedDate != null;
        timeInField.setEnabled(timeEnabled);
        timeOutField.setEnabled(timeEnabled && student.timeIn != null
                && (student.status == Status.PRESENT || student.status == Status.LATE));
        timeInField.refresh();
        timeOutField.refresh();
        setEnabled(panel.selectedDate != null);
        repaint();
    }

    // Calculates the available width for the student name.
    private int textWidthForCard() {
        int width;
        if (getWidth() > 0) {
            width = getWidth();
        } else {
            width = getPreferredSize().width;
        }
        if (getWidth() <= 0) {
            width = estimatedGridCardWidth();
        }
        return Math.max(s(150), width - s(66));
    }

    // Estimates the card width before the grid has finished layout.
    private int estimatedGridCardWidth() {
        if (panel.gridPanel == null) {
            return getPreferredSize().width;
        }

        int availableWidth = panel.gridPanel.getWidth();
        Container parent = panel.gridPanel.getParent();
        // Uses the scroll pane viewport width when the grid itself is not sized yet.
        if (parent instanceof JViewport && parent.getWidth() > 0) {
            JViewport viewport = (JViewport) parent;
            availableWidth = viewport.getWidth() - s(28);
        }
        if (availableWidth <= 0) {
            availableWidth = Math.max(s(520), panel.getWidth() - s(56));
        }

        int gap = s(22);
        int columns = panel.calculateGridColumns();
        return Math.max(getPreferredSize().width, (availableWidth - gap * Math.max(0, columns - 1)) / columns);
    }

    // Paints the student card background, accent strip, and warning dot.
    @Override
    protected void paintComponent(Graphics g) {
        // This draws the card background, hover shadow, side strip, and warning dots.
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double progress = 0.0;
        if (isEnabled()) {
            progress = hoverProgress;
        }
        int inset = (int) Math.round(s(5) * (1.0 - progress));
        int x = inset;
        int y = inset;
        int cardW = getWidth() - inset * 2;
        int cardH = getHeight() - inset * 2;
        int arc = s(14);

        if (progress > 0.0) {
            int alpha = 16 + (int) Math.round(26 * progress);
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRoundRect(x, y + s(3), cardW, cardH, arc, arc);
        }

        if (pressed) {
            x += s(2);
            y += s(2);
            cardW -= s(4);
            cardH -= s(4);
        }

        Color fill;
        if (pressed) {
            fill = StartSessionPanel.CARD_PRESSED_BG;
        } else {
            fill = panel.blend(StartSessionPanel.CARD_BG, StartSessionPanel.CARD_HOVER_BG, progress);
        }
        java.awt.geom.RoundRectangle2D.Float cardShape = new java.awt.geom.RoundRectangle2D.Float(x, y, cardW,
                cardH, arc, arc);
        g2.setColor(fill);
        g2.fill(cardShape);

        if (progress > 0.0 && !pressed) {
            Color accent = student.status.color;
            // Soft color glow from the left side during hover.
            g2.setPaint(new GradientPaint(x, y, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    28 + (int) Math.round(24 * progress)), x + s(92), y,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0)));
            g2.fill(cardShape);
        }

        Color strip;
        if (pressed) {
            strip = panel.blend(student.status.color, StartSessionPanel.CARD_PRESSED_BG, 0.25);
        } else {
            strip = panel.blend(student.status.color, StartSessionPanel.TEXT, progress * 0.18);
        }
        g2.setColor(strip);
        g2.fillRoundRect(x, y, s(5 + (int) Math.round(progress * 2)), cardH, arc, arc);

        if (student.isFda()) {
            // Red dot means FDA level absences.
            g2.setColor(StartSessionPanel.BRAND_RED);
            g2.fillOval(x + cardW - s(18), y + s(12), s(8), s(8));
        } else if (student.isWarning()) {
            // Orange dot means warning level absences.
            g2.setColor(StartSessionPanel.ORANGE);
            g2.fillOval(x + cardW - s(18), y + s(12), s(8), s(8));
        }

        g2.dispose();
        super.paintComponent(g);
    }

    // Converts a base size into a scaled size.
    private int s(int base) {
        return panel.s(base);
    }

    // Small pill that displays the student's current attendance status.
    private final class StatusBadge extends JComponent {
        private final StudentRecord student;

        // Builds the small status badge for one student.
        private StatusBadge(StudentRecord student) {
            this.student = student;
            Dimension size = new Dimension(s(100), s(20));
            panel.setFixedSize(this, size);
        }

        // Repaints the status badge.
        private void refresh() {
            repaint();
        }

        // Paints the status badge pill and dot.
        @Override
        protected void paintComponent(Graphics g) {
            // Draws the rounded status pill and the small colored status dot.
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float stroke = Math.max(1f, s(1));
            float inset = stroke / 2f + 0.5f;
            float width = Math.max(0f, getWidth() - inset * 2f);
            float height = Math.max(0f, getHeight() - inset * 2f);
            float arc = Math.min(s(StartSessionPanel.CONTROL_RADIUS), height);
            java.awt.geom.RoundRectangle2D.Float pill = new java.awt.geom.RoundRectangle2D.Float(inset, inset, width,
                    height, arc, arc);

            g2.setColor(StartSessionPanel.BADGE_BG);
            g2.fill(pill);
            g2.setColor(StartSessionPanel.BADGE_BORDER);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(pill);
            g2.setColor(student.status.color);
            int dot = s(6);
            g2.fillOval(s(8), (getHeight() - dot) / 2, dot, dot);
            g2.setColor(StartSessionPanel.TEXT);
            g2.setFont(panel.font(Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(student.status.label, s(22), textY);
            g2.dispose();
        }
    }

    // Time In/Time Out field inside each student card.
    final class TimeEntryField extends JTextField {
        private final StudentRecord student;
        // true means this field edits Time In, false means Time Out.
        private final boolean timeIn;
        private boolean editing;

        // Builds the Time In or Time Out field inside a student card.
        private TimeEntryField(StudentRecord student, boolean timeIn) {
            this.student = student;
            this.timeIn = timeIn;
            setOpaque(false);
            setBorder(new EmptyBorder(0, s(12), 0, s(12)));
            setForeground(StartSessionPanel.TEXT);
            setDisabledTextColor(StartSessionPanel.META);
            setCaretColor(StartSessionPanel.TEXT);
            setSelectionColor(StartSessionPanel.TEXT_SELECTION_BG);
            setFont(panel.font(Font.BOLD, 13));
            setHorizontalAlignment(SwingConstants.CENTER);
            setEditable(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(panel.studentTimeFieldName(timeIn) + " - click to set, click again to edit");
            addActionListener(e -> panel.commitStudentTimeField(student, timeIn, TimeEntryField.this));
            addMouseListener(new MouseAdapter() {
                // Handles clicking the student time field.
                @Override
                public void mousePressed(MouseEvent e) {
                    // Clicking the field can set current time or start manual editing.
                    if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                        e.consume();
                        panel.handleTimeFieldActivation(student, timeIn, TimeEntryField.this);
                    }
                }
            });
            addFocusListener(new FocusAdapter() {
                // Finishes editing when the time field loses focus.
                @Override
                public void focusLost(FocusEvent e) {
                    // Stop edit mode if the user clicks somewhere else.
                    finishEdit();
                }
            });
            refresh();
        }

        // Checks if this field already has a saved time value.
        boolean hasTimeValue() {
            return studentTimeValue() != null;
        }

        // Turns the display field into an editable text field.
        void beginEdit() {
            // Turns the display-like field into a normal editable text field.
            if (!isEnabled()) {
                return;
            }
            editing = true;
            LocalTime value = studentTimeValue();
            if (value == null) {
                setText("");
            } else {
                setText(panel.formatTime(value));
            }
            setEditable(true);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            requestFocusInWindow();
            selectAll();
            repaint();
        }

        // Turns the editable text field back into display mode.
        void finishEdit() {
            // Returns the text field back to button-like display mode.
            editing = false;
            setEditable(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            refresh();
        }

        // Refreshes the displayed Time In or Time Out text.
        void refresh() {
            if (editing) {
                repaint();
                return;
            }
            LocalTime value = studentTimeValue();
            setText(panel.cardTimeText(timeIn, value));
            repaint();
        }

        // Returns this field's student time value.
        private LocalTime studentTimeValue() {
            if (timeIn) {
                return student.timeIn;
            }
            return student.timeOut;
        }

        // Paints the student time field background and border.
        @Override
        protected void paintComponent(Graphics g) {
            // Draws the small rounded Time In/Time Out box inside a student card.
            panel.paintRoundedInput(g, this, isEnabled(), 8);
            super.paintComponent(g);
        }
    }
}
