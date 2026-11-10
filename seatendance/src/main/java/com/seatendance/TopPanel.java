package com.seatendance;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

public class TopPanel extends JPanel {

    protected final double scale;
    Point dragStart = new Point();

    protected int s(int base) {
        return (int) (base * scale);
    }

    private void addDragListener(JFrame currentFrame) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getLocationOnScreen();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = e.getLocationOnScreen();
                Point frameLocation = currentFrame.getLocation();
                currentFrame.setLocation(
                        frameLocation.x + current.x - dragStart.x,
                        frameLocation.y + current.y - dragStart.y);
                dragStart = current;
            }
        });
    }

    public TopPanel(JFrame currentFrame, double scale, Color bgColor) {
        this.scale = scale;
        setBackground(bgColor);
        addDragListener(currentFrame);
    }

    public TopPanel(JFrame currentFrame, double scale, Color fontColor, String titleString, Color bgColor,
            int fontSize) {
        this.scale = scale;

        setBackground(bgColor);
        setLayout(new FlowLayout(FlowLayout.LEFT));

        addDragListener(currentFrame);

        addLogoAndTitleTo(this, fontColor, titleString, fontSize);
    }

    public TopPanel(JFrame currentFrame, double scale, Color fontColor, boolean minimize, boolean maximize,
            boolean close, Color bgColor, int fontSize, Color buttonColor, boolean hover, Color fontHoverColor,
            Color buttonHoverColor) {
        this.scale = scale;

        setBackground(bgColor);
        setLayout(new FlowLayout(FlowLayout.RIGHT));

        addDragListener(currentFrame);

        if (minimize) {
            addMinimizeButtonTo(this, currentFrame, buttonColor, fontColor, fontSize, hover, fontHoverColor,
                    buttonHoverColor);
        }

        if (maximize) {
            addMaximizeButtonTo(this, currentFrame, buttonColor, fontColor, fontSize, hover, fontHoverColor,
                    buttonHoverColor);
        }

        if (close) {
            addCloseButtonTo(this, currentFrame, buttonColor, fontColor, fontSize, hover, fontHoverColor);
        }

    }

    public TopPanel(JFrame currentFrame, double scale, Color fontColor, String titleString, boolean minimize,
            boolean maximize, boolean close, Color bgColor, int fontSize, Color buttonColor, boolean hover,
            Color fontHoverColor, Color buttonHoverColor) {
        this.scale = scale;

        setBackground(bgColor);
        setLayout(new BorderLayout());

        addDragListener(currentFrame);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, s(5), 0));
        titleRow.setBackground(bgColor);
        addLogoAndTitleTo(titleRow, fontColor, titleString, fontSize);

        JPanel leftPanel = new JPanel(new GridBagLayout()); // GridBagLayout Centers the title
        leftPanel.setBackground(bgColor);
        leftPanel.add(titleRow);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(bgColor);

        if (minimize) {
            addMinimizeButtonTo(rightPanel, currentFrame, buttonColor, fontColor, fontSize, hover, fontHoverColor,
                    buttonHoverColor);
        }

        if (maximize) {
            addMaximizeButtonTo(rightPanel, currentFrame, buttonColor, fontColor, fontSize, hover, fontHoverColor,
                    buttonHoverColor);
        }

        if (close) {
            addCloseButtonTo(rightPanel, currentFrame, buttonColor, fontColor, fontSize, hover, fontHoverColor);
        }

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    private void addLogoAndTitleTo(JPanel panel, Color fontColor, String titleString, int fontSize) {
        Image scaled = new ImageIcon(getClass().getResource("/images/smallSeatendanceLogo.png")).getImage()
                .getScaledInstance(s(20), s(20), Image.SCALE_SMOOTH);

        JLabel logoLabel = new JLabel(new ImageIcon(scaled)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                super.paintComponent(g2d);
            }
        };

        JLabel titleLabel = new JLabel(titleString);
        titleLabel.setForeground(fontColor);
        titleLabel.setFont(new Font(null, Font.PLAIN, fontSize));

        panel.add(logoLabel);
        panel.add(titleLabel);
    }

    private void addMinimizeButtonTo(JPanel panel, JFrame currentFrame, Color buttonColor, Color fontColor,
            int fontSize, boolean hover, Color fontHoverColor, Color buttonHoverColor) {
        JButton minimizeButton = new JButton("—");
        minimizeButton.setToolTipText("Minimize");

        minimizeButton.setContentAreaFilled(false);
        minimizeButton.setBorderPainted(false);
        minimizeButton.setOpaque(true);
        minimizeButton.setFocusable(false);
        minimizeButton.setFont(new Font(null, Font.PLAIN, fontSize));
        minimizeButton.setBackground(buttonColor);
        minimizeButton.setForeground(fontColor);

        minimizeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (hover) {
                    minimizeButton.setBackground(buttonHoverColor);
                    minimizeButton.setForeground(fontHoverColor);
                } else {
                    minimizeButton.setBackground(buttonColor);
                    minimizeButton.setForeground(fontColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                minimizeButton.setBackground(buttonColor);
                minimizeButton.setForeground(fontColor);
            }
        });

        minimizeButton.addActionListener(e -> currentFrame.setState(Frame.ICONIFIED));
        panel.add(minimizeButton);
    }

    private void addMaximizeButtonTo(JPanel panel, JFrame currentFrame, Color buttonColor, Color fontColor,
            int fontSize, boolean hover, Color fontHoverColor, Color buttonHoverColor) {
        JButton maximizeButton = new JButton("▢");
        maximizeButton.setToolTipText("Maximize");

        maximizeButton.setContentAreaFilled(false);
        maximizeButton.setBorderPainted(false);
        maximizeButton.setOpaque(true);
        maximizeButton.setFocusable(false);
        maximizeButton.setFont(new Font(null, Font.PLAIN, fontSize));
        maximizeButton.setBackground(buttonColor);
        maximizeButton.setForeground(fontColor);

        maximizeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (hover) {
                    maximizeButton.setBackground(buttonHoverColor);
                    maximizeButton.setForeground(fontHoverColor);
                } else {
                    maximizeButton.setBackground(buttonColor);
                    maximizeButton.setForeground(fontColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                maximizeButton.setBackground(buttonColor);
                maximizeButton.setForeground(fontColor);
            }
        });

        maximizeButton.addActionListener(e -> {
            if (currentFrame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                currentFrame.setExtendedState(Frame.NORMAL);
                maximizeButton.setText("▢");
            } else {
                currentFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
                maximizeButton.setText("❐");
            }
        });

        panel.add(maximizeButton);
    }

    static final Color closeHoverColor = new Color(255, 0, 0);

    private void addCloseButtonTo(JPanel panel, JFrame currentFrame, Color buttonColor, Color fontColor, int fontSize,
            boolean hover, Color fontHoverColor) {
        JButton closeButton = new JButton("✕");
        closeButton.setToolTipText("Close");

        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setOpaque(true);
        closeButton.setFocusable(false);
        closeButton.setFont(new Font(null, Font.PLAIN, fontSize));
        closeButton.setBackground(buttonColor);
        closeButton.setForeground(fontColor);

        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (hover) {
                    closeButton.setBackground(closeHoverColor);
                    closeButton.setForeground(fontHoverColor);
                } else {
                    closeButton.setBackground(buttonColor);
                    closeButton.setForeground(fontColor);
                }
                closeButton.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(buttonColor);
                closeButton.setForeground(fontColor);
                closeButton.repaint();
            }
        });

        closeButton.addActionListener(e -> {
            currentFrame.dispose();
            System.exit(0);
        });

        panel.add(closeButton);
    }
}