package com.seatendance;

import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SeatendanceSplash extends ScaledWindow {
    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------
    static final Color maroon = new Color(128, 0, 0);
    static final Color maroonDark = new Color(90, 0, 0);
    static final Color maroonDeep = new Color(60, 0, 0);
    static final Color white = Color.WHITE;
    static final Color black = Color.BLACK;
    static final Color darkGray = new Color(30, 30, 30);

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    public SeatendanceSplash(MainSeatendanceApp MainSeatendanceApp, double scale) {
        super(scale);
        // --- Size Calculation ---
        int basePanelWidth = 600;
        int basePanelHeight = 424;
        int panelWidth = s(basePanelWidth);
        int panelHeight = s(basePanelHeight);
        // --- Window Setup ---
        setBackground(maroonDeep);
        setAlwaysOnTop(true);
        // -------------------------------------------------------------------------
        // Close Button
        // -------------------------------------------------------------------------
        JButton closeButton = new JButton("✕") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setOpaque(false);
        closeButton.setFocusable(false);
        closeButton.setBackground(maroonDark);
        closeButton.setForeground(Color.BLACK);
        // --- Close Button Action ---
        closeButton.addActionListener(e -> {
            if (dotTimer != null)
                dotTimer.stop();
            if (worker != null)
                worker.cancel(true);
            dispose();
            System.exit(0);
        });
        // --- Hover Effects ---
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(maroonDeep);
                closeButton.setForeground(new Color(200, 200, 200, 255));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(maroonDark);
                closeButton.setForeground(new Color(0, 0, 0, 255));
            }
        });
        // -------------------------------------------------------------------------
        // Top Bar
        // -------------------------------------------------------------------------
        JPanel windowPanel = new JPanel();
        windowPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        windowPanel.setOpaque(false);
        windowPanel.add(closeButton);
        // -------------------------------------------------------------------------
        // Logo
        // -------------------------------------------------------------------------
        Image scaled = new ImageIcon(getClass().getResource("/images/smallSeatendanceLogo.png")).getImage()
                .getScaledInstance(s(100), s(100), Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaled)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                super.paintComponent(g2d);
            }
        };
        logoLabel.setAlignmentX(CENTER_ALIGNMENT);
        // -------------------------------------------------------------------------
        // Title & Tagline Labels
        // -------------------------------------------------------------------------
        int baseTitleSize = 40;
        int baseTaglineSize = 10;
        JLabel titleLabel = new JLabel("SEATENDANCE");
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        titleLabel.setForeground(white);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, s(baseTitleSize)));
        JLabel taglineLabel = new JLabel("SEATING ARRANGEMENT AND ATTENDANCE MANAGEMENT SYSTEM");
        taglineLabel.setFont(new Font("Verdana", Font.PLAIN, s(baseTaglineSize)));
        taglineLabel.setForeground(new Color(255, 255, 255, 100));
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        // -------------------------------------------------------------------------
        // Main Splash Panel
        // -------------------------------------------------------------------------
        JPanel mainSplashPanel = new JPanel();
        mainSplashPanel.setOpaque(false);
        mainSplashPanel.setLayout(new BoxLayout(mainSplashPanel, BoxLayout.Y_AXIS));
        mainSplashPanel.add(Box.createVerticalGlue());
        mainSplashPanel.add(logoLabel);
        mainSplashPanel.add(Box.createVerticalStrut(s(10)));
        mainSplashPanel.add(titleLabel);
        mainSplashPanel.add(taglineLabel);
        mainSplashPanel.add(Box.createVerticalGlue());
        // -------------------------------------------------------------------------
        // Progress Bar
        // -------------------------------------------------------------------------
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, s(3)));
        progressBar.setOpaque(true);
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                int width = progressBar.getWidth();
                int height = progressBar.getHeight();
                int filled = (int) ((progressBar.getValue() / 100.0) * width);
                g.setColor(black);
                g.fillRect(0, 0, width, height);
                g.setColor(white);
                g.fillRect(0, 0, filled, height);
            }
        });
        progressBar.setValue(0);
        JPanel progressBarPanel = new JPanel(new BorderLayout());
        progressBarPanel.setOpaque(false);
        progressBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, s(24), 0));
        progressBarPanel.add(progressBar, BorderLayout.CENTER);
        // -------------------------------------------------------------------------
        // Loading + Percentage Row
        // -------------------------------------------------------------------------
        int baseLoadingSize = 11;
        JLabel loadingLabel = new JLabel("Loading");
        loadingLabel.setForeground(white);
        loadingLabel.setFont(new Font("Verdana", Font.PLAIN, s(baseLoadingSize)));
        JLabel percentLabel = new JLabel("0%");
        percentLabel.setForeground(white);
        percentLabel.setFont(new Font("Verdana", Font.PLAIN, s(baseLoadingSize)));
        JPanel loadingPercentPanel = new JPanel(new BorderLayout());
        loadingPercentPanel.setOpaque(false);
        loadingPercentPanel.add(loadingLabel, BorderLayout.WEST);
        loadingPercentPanel.add(percentLabel, BorderLayout.EAST);
        // -------------------------------------------------------------------------
        // Progress Panel
        // -------------------------------------------------------------------------
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(0, s(48), 0, s(48)));
        progressPanel.setOpaque(false);
        progressPanel.add(loadingPercentPanel);
        progressPanel.add(Box.createVerticalStrut(s(5)));
        progressPanel.add(progressBarPanel);
        // -------------------------------------------------------------------------
        // Content Panel
        // -------------------------------------------------------------------------
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                RadialGradientPaint gradient = new RadialGradientPaint(
                        w / 2f, h / 2f,
                        Math.max(w, h) / 1.5f,
                        new float[] { 0f, 1f },
                        new Color[] { maroon, maroonDeep });
                g2.setPaint(gradient);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        contentPanel.setBackground(maroonDeep);
        contentPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
        contentPanel.setLayout(new BorderLayout());
        // -------------------------------------------------------------------------
        // Content Panel Assembly
        // -------------------------------------------------------------------------
        contentPanel.add(windowPanel, BorderLayout.NORTH);
        contentPanel.add(mainSplashPanel, BorderLayout.CENTER);
        contentPanel.add(progressPanel, BorderLayout.SOUTH);
        // -------------------------------------------------------------------------
        // Launch
        // -------------------------------------------------------------------------
        startLoading(progressBar, loadingLabel, percentLabel, MainSeatendanceApp);
        setContentPane(contentPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private Timer dotTimer;
    private SwingWorker<Void, Integer> worker;

    // -------------------------------------------------------------------------
    // Loading Logic
    // -------------------------------------------------------------------------
    private void startLoading(JProgressBar progressBar, JLabel loadingLabel, JLabel percentLabel,
            MainSeatendanceApp MainSeatendanceApp) {
        // --- Dot Animation ---
        final int[] dotCount = { 0 };
        dotTimer = new Timer(300, e -> {
            dotCount[0] = (dotCount[0] + 1) % 4;
            loadingLabel.setText("Loading" + ".".repeat(dotCount[0]));
        });
        dotTimer.start();
        // --- Background Worker ---
        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                DatabaseHandler db = MainSeatendanceApp.getDatabase();

                publish(0);
                Thread.sleep(200);

                db.createDatabase();
                publish(25);
                Thread.sleep(200);

                db.createTables();
                db.seedDefaultAdmin();
                publish(50);
                Thread.sleep(200);

                try {
                    MainSeatendanceApp.startRFID();
                } catch (Throwable e) {
                    System.err.println("RFID scanner startup skipped.");
                    e.printStackTrace();
                }
                publish(75);
                Thread.sleep(200);

                publish(100);
                Thread.sleep(200);

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                Integer latestProgress = chunks.get(chunks.size() - 1);
                progressBar.setValue(latestProgress);
                percentLabel.setText(latestProgress + "%");
            }

            @Override
            protected void done() {
                if (isCancelled())
                    return;
                try {
                    get();
                } catch (Exception e) {
                    dotTimer.stop();
                    loadingLabel.setText("Error loading!");
                    e.printStackTrace();
                    return;
                }
                dotTimer.stop();
                loadingLabel.setText("Loading complete!");
                // --- Delay Before Transition ---
                Timer delayTimer = new Timer(1000, e -> {
                    dispose();
                    MainSeatendanceApp.showLogin();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            }
        };
        worker.execute();
    }
}
