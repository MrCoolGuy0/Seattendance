package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

public class Seatendance extends ScaledFrame {

    public JButton editDatabaseButton;

    private JPanel contentHost;
    private CardLayout cardLayout = new CardLayout();
    private AbstractButton activeNavButton;
    private AbstractButton dashboardNavButton;
    private AbstractButton startSessionNavButton;
    private AbstractButton attendanceReportNavButton;
    private AbstractButton classroomBuilderNavButton;
    private AbstractButton editStudentPermissionsButton;
    private AttendanceReport attendanceReportPanel;

    private JPanel recentActivityRows;
    private static final int MAX_RECENT = 8;

    private static final Color WINDOW_BG = new Color(18, 23, 26);
    private static final Color TITLE_BG = new Color(22, 27, 30);
    private static final Color SIDEBAR_BG = new Color(19, 24, 27);
    private static final Color CONTENT_BG = new Color(34, 38, 42);
    private static final Color CARD_BG = new Color(20, 25, 28);
    private static final Color CONTROL_BG = new Color(36, 41, 46);
    private static final Color CONTROL_HOVER_BG = new Color(45, 51, 57);
    private static final Color BORDER = new Color(63, 70, 78);
    private static final Color TEXT = Color.WHITE;
    private static final Color MUTED_TEXT = new Color(174, 182, 193);
    private static final Color RED = new Color(190, 40, 49);
    private static final Color DEEP_RED = new Color(72, 29, 34);
    private static final Color STAT_RED = new Color(111, 34, 36);
    private static final Color STAT_GREEN = new Color(43, 83, 63);
    private static final Color STAT_GOLD = new Color(113, 76, 28);
    private static final Color STAT_ABSENT = new Color(124, 42, 42);

    private String currentUserEmail;
    private MainSeatendanceApp seatendanceApp;
    private String role;
    private StartSessionPanel startSessionPanel;
    private ClassroomBuilder classroomBuilderPanel;

    private DatabaseHandler db;

    public Seatendance(MainSeatendanceApp mainSeatendanceApp, double scale, String email, String role) {
        super(scale);
        this.currentUserEmail = email;
        this.seatendanceApp = mainSeatendanceApp;
        this.role = role;
        db = mainSeatendanceApp.getDatabase();

        setTitle("Seatendance");
        setUndecorated(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(s(1535), s(1024)));
        setMinimumSize(new Dimension(s(1080), s(720)));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(WINDOW_BG);
        root.setBorder(new LineBorder(BORDER, 1));

        root.add(createWindowTopPanel(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(createSidebar(db), BorderLayout.WEST);

        contentHost = new JPanel(cardLayout);
        contentHost.setOpaque(false);

        contentHost.add(createDashboard(), "dashboard");

        startSessionPanel = new StartSessionPanel(seatendanceApp, scale,
                db.getClassOptionsForUser(currentUserEmail), currentUserEmail);
        contentHost.add(startSessionPanel, "startSession");

        contentHost.add(new DatabaseEditor(db, scale, role, seatendanceApp.getRFIDScanner()), "databaseEditor");

        attendanceReportPanel = new AttendanceReport(db, scale,
                db.getClassOptionsForUser(currentUserEmail), currentUserEmail);
        contentHost.add(attendanceReportPanel, "attendanceReport");

        contentHost.add(new Permissions(scale, seatendanceApp, db), "permissions");

        String tName = getLoggedInTeacherName();
        classroomBuilderPanel = new ClassroomBuilder((float) scale, db, null, false, null, tName);
        classroomBuilderPanel.setApp(seatendanceApp);
        contentHost.add(classroomBuilderPanel, "classroomBuilder");

        body.add(contentHost, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        setAlwaysOnTop(true);
        toFront();
        requestFocus();
        setAlwaysOnTop(false);
    }

    private JPanel createWindowTopPanel() {
        TopPanel titleBar = new TopPanel(
                this,
                scale,
                TEXT,
                "Seatendance",
                true,
                true,
                true,
                TITLE_BG,
                s(17),
                TITLE_BG,
                true,
                TEXT,
                new Color(43, 49, 54));
        titleBar.setPreferredSize(new Dimension(0, s(50)));
        titleBar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        return titleBar;
    }

    private JPanel createSidebar(DatabaseHandler db) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(s(354), 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(s(14), s(18), 0, s(20)));

        top.add(createProgramCard());
        top.add(Box.createVerticalStrut(s(18)));

        dashboardNavButton = createNavButton("Dashboard", IconAsset.DASHBOARD, CONTROL_BG, CONTROL_HOVER_BG);
        startSessionNavButton = createNavButton("Start Session", IconAsset.START_SESSION, CONTROL_BG, CONTROL_HOVER_BG);

        if (role.equals("admin")) {
            editDatabaseButton = createNavButton("Edit Database", IconAsset.STUDENTS, CONTROL_BG, CONTROL_HOVER_BG);
        } else {
            editDatabaseButton = createNavButton("Students", IconAsset.STUDENTS, CONTROL_BG, CONTROL_HOVER_BG);
        }

        classroomBuilderNavButton = createNavButton("Classrooms", IconAsset.CLASSROOMS, CONTROL_BG, CONTROL_HOVER_BG);
        attendanceReportNavButton = createNavButton("Reports", IconAsset.REPORTS, CONTROL_BG, CONTROL_HOVER_BG);
        editStudentPermissionsButton = createNavButton("Permissions", IconAsset.PERMISSION, CONTROL_BG,
                CONTROL_HOVER_BG);
        editStudentPermissionsButton.setVisible(false);
        JButton logoutButton = createNavButton("Log Out", IconAsset.LOGOUT, CONTROL_BG, CONTROL_HOVER_BG);

        dashboardNavButton.addActionListener(e -> showDashboard());
        startSessionNavButton.addActionListener(e -> showStartSession());
        editDatabaseButton.addActionListener(e -> showDatabaseEditor());
        attendanceReportNavButton.addActionListener(e -> showAttendanceReport());
        classroomBuilderNavButton.addActionListener(e -> showClassroomBuilder());
        editStudentPermissionsButton.addActionListener(e -> showPermissions());
        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to log out?",
                    "Log Out",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                seatendanceApp.showLogin();
            }
        });

        setActiveNav(dashboardNavButton);

        if (role.equals("admin") || role.equals("teacher")) {
            top.add(dashboardNavButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(startSessionNavButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(editDatabaseButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(classroomBuilderNavButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(attendanceReportNavButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(editStudentPermissionsButton);
        } else if (role.equals("student")) {
            top.add(dashboardNavButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(startSessionNavButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(editDatabaseButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(classroomBuilderNavButton);
            top.add(Box.createVerticalStrut(s(8)));
            top.add(attendanceReportNavButton);
        }

        JPanel settingsArea = new JPanel(new BorderLayout());
        settingsArea.setOpaque(false);
        settingsArea.setBorder(new EmptyBorder(0, s(18), s(32), s(20)));

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(45, 51, 56));
        separator.setBackground(new Color(45, 51, 56));

        JPanel settingsContent = new JPanel();
        settingsContent.setOpaque(false);
        settingsContent.setLayout(new BoxLayout(settingsContent, BoxLayout.Y_AXIS));
        settingsContent.add(separator);
        settingsContent.add(Box.createVerticalStrut(s(16)));
        settingsContent.add(logoutButton);

        settingsArea.add(settingsContent, BorderLayout.CENTER);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(settingsArea, BorderLayout.SOUTH);
        return sidebar;
    }

    private void showDashboard() {
        setActiveNav(dashboardNavButton);
        cardLayout.show(contentHost, "dashboard");
    }

    private void showStartSession() {
        setActiveNav(startSessionNavButton);
        cardLayout.show(contentHost, "startSession");
    }

    private void setActiveNav(AbstractButton button) {
        if (activeNavButton instanceof NavButton navButton) {
            navButton.setActive(false);
        }

        activeNavButton = button;
        if (activeNavButton instanceof NavButton navButton) {
            navButton.setActive(true);
        }
    }

    private JPanel createProgramCard() {
        RoundedPanel card = new RoundedPanel(CARD_BG, new Color(72, 78, 86), 12, 1);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(s(314), s(119)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(119)));
        card.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel logo = createImageLabel("/images/ProgramLogos/CCS.png", 88, 88);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(Box.createVerticalGlue());

        JLabel program = new JLabel("Computer Science");
        program.setForeground(TEXT);
        program.setFont(font(Font.BOLD, 18));
        JLabel section = new JLabel("BSCS");
        section.setForeground(new Color(146, 155, 166));
        section.setFont(font(Font.PLAIN, 15));

        textPanel.add(program);
        textPanel.add(Box.createVerticalStrut(s(5)));
        textPanel.add(section);
        textPanel.add(Box.createVerticalGlue());

        JPanel row = new JPanel(new BorderLayout(s(18), 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(s(14), s(14), s(14), s(14)));
        row.add(logo, BorderLayout.WEST);
        row.add(textPanel, BorderLayout.CENTER);

        card.add(new AccentStrip(RED, 12), BorderLayout.WEST);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDashboard() {
        String name = seatendanceApp.getDatabase().getUserName(currentUserEmail);

        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.setBackground(CONTENT_BG);
        dashboard.setBorder(new EmptyBorder(s(32), s(30), s(36), s(28)));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome back, " + name);
        welcome.setForeground(TEXT);
        welcome.setFont(font(Font.BOLD, 32));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Today's attendance overview");
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(font(Font.PLAIN, 20));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(welcome);
        header.add(Box.createVerticalStrut(s(5)));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(s(20)));

        dashboard.add(header, BorderLayout.NORTH);
        dashboard.add(createDashboardBodyPanel(), BorderLayout.CENTER);

        return dashboard;
    }

    private JPanel createDashboardBodyPanel() {
        RoundedPanel panel = new RoundedPanel(CARD_BG, BORDER, 10, 1);
        panel.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(s(16), s(20), s(12), s(20)));

        JLabel title = new JLabel("Recent Time-ins");
        title.setForeground(TEXT);
        title.setFont(font(Font.BOLD, 18));

        JPanel cols = new JPanel(new GridLayout(1, 5));
        cols.setOpaque(false);
        cols.setBorder(new EmptyBorder(s(8), 0, 0, 0));
        String[] colHeaders = { "Student", "Subject / Section", "Status", "Action", "Time" };
        for (String h : colHeaders) {
            JLabel lbl = new JLabel(h);
            lbl.setForeground(MUTED_TEXT);
            lbl.setFont(font(Font.PLAIN, 14));
            cols.add(lbl);
        }

        header.add(title, BorderLayout.NORTH);
        header.add(cols, BorderLayout.SOUTH);

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setBackground(BORDER);

        JPanel headerBlock = new JPanel(new BorderLayout());
        headerBlock.setOpaque(false);
        headerBlock.add(header, BorderLayout.CENTER);
        headerBlock.add(sep, BorderLayout.SOUTH);

        recentActivityRows = new JPanel();
        recentActivityRows.setOpaque(false);
        recentActivityRows.setLayout(new BoxLayout(recentActivityRows, BoxLayout.Y_AXIS));
        recentActivityRows.setBorder(new EmptyBorder(s(4), s(20), s(12), s(20)));

        JPanel emptyWrapper = new JPanel(new BorderLayout());
        emptyWrapper.setOpaque(false);
        emptyWrapper.setBorder(new EmptyBorder(s(24), 0, s(24), 0));
        JLabel emptyLbl = new JLabel("No recent time-ins yet.", SwingConstants.CENTER);
        emptyLbl.setForeground(MUTED_TEXT);
        emptyLbl.setFont(font(Font.ITALIC, 15));
        emptyWrapper.add(emptyLbl, BorderLayout.CENTER);
        recentActivityRows.add(emptyWrapper);

        JScrollPane scroll = new JScrollPane(recentActivityRows);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(s(12));

        panel.add(headerBlock, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    public void pushRecentActivity(String studentName, String subjectInfo, String status, String action, String time) {
        if (!"Present".equalsIgnoreCase(status) && !"Late".equalsIgnoreCase(status)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (recentActivityRows.getComponentCount() == 1) {
                Component first = recentActivityRows.getComponent(0);
                if (first instanceof JPanel p && p.getComponentCount() > 0
                        && p.getComponent(0) instanceof JLabel lbl
                        && lbl.getText().contains("No recent")) {
                    recentActivityRows.removeAll();
                }
            }

            JPanel row = buildActivityRow(studentName, subjectInfo, status, action, time);
            recentActivityRows.add(row, 0);

            while (recentActivityRows.getComponentCount() > MAX_RECENT) {
                recentActivityRows.remove(recentActivityRows.getComponentCount() - 1);
            }

            recentActivityRows.revalidate();
            recentActivityRows.repaint();
        });
    }

    private JPanel buildActivityRow(String studentName, String subjectInfo, String status, String action, String time) {
        JPanel row = new JPanel(new GridLayout(1, 5, s(8), 0));
        row.setOpaque(false);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(45, 51, 56)),
                new EmptyBorder(s(10), 0, s(10), 0)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(52)));

        JLabel nameLbl = new JLabel(studentName);
        nameLbl.setForeground(TEXT);
        nameLbl.setFont(font(Font.BOLD, 15));

        JLabel subjectLbl = new JLabel(subjectInfo);
        subjectLbl.setForeground(MUTED_TEXT);
        subjectLbl.setFont(font(Font.PLAIN, 14));

        Color pillBg;
        Color pillFg;
        switch (status.toLowerCase()) {
            case "late":
                pillBg = STAT_GOLD;
                pillFg = new Color(255, 200, 100);
                break;
            case "present":
            default:
                pillBg = STAT_GREEN;
                pillFg = new Color(120, 220, 160);
                break;
        }

        JLabel statusLbl = new JLabel(status) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pillBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(8), s(8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusLbl.setForeground(pillFg);
        statusLbl.setFont(font(Font.BOLD, 13));
        statusLbl.setHorizontalAlignment(SwingConstants.CENTER);
        statusLbl.setOpaque(false);
        statusLbl.setPreferredSize(new Dimension(s(80), s(28)));
        statusLbl.setMaximumSize(new Dimension(s(80), s(28)));

        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusWrapper.setOpaque(false);
        statusWrapper.add(statusLbl);

        boolean isTimeOut = "Time Out".equalsIgnoreCase(action);
        Color actionBg = isTimeOut ? new Color(50, 60, 80) : new Color(30, 55, 75);
        Color actionFg = isTimeOut ? new Color(160, 185, 220) : new Color(100, 180, 255);

        JLabel actionLbl = new JLabel(action) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(actionBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(8), s(8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        actionLbl.setForeground(actionFg);
        actionLbl.setFont(font(Font.BOLD, 13));
        actionLbl.setHorizontalAlignment(SwingConstants.CENTER);
        actionLbl.setOpaque(false);
        actionLbl.setPreferredSize(new Dimension(s(80), s(28)));
        actionLbl.setMaximumSize(new Dimension(s(80), s(28)));

        JPanel actionWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionWrapper.setOpaque(false);
        actionWrapper.add(actionLbl);

        JLabel timeLbl = new JLabel(time);
        timeLbl.setForeground(MUTED_TEXT);
        timeLbl.setFont(font(Font.PLAIN, 14));

        row.add(nameLbl);
        row.add(subjectLbl);
        row.add(statusWrapper);
        row.add(actionWrapper);
        row.add(timeLbl);

        return row;
    }

    private JButton createNavButton(String text, IconAsset iconAsset, Color background, Color hoverBackground) {
        JButton button = new NavButton(text, iconAsset, background, hoverBackground);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(70)));
        button.setPreferredSize(new Dimension(s(316), s(70)));
        return button;
    }

    private JLabel createImageLabel(String path, int width, int height) {
        java.net.URL resource = getClass().getResource(path);
        if (resource == null) {
            JLabel fallback = new JLabel();
            fallback.setPreferredSize(new Dimension(s(width), s(height)));
            return fallback;
        }

        Image image = new ImageIcon(resource).getImage().getScaledInstance(s(width), s(height), Image.SCALE_SMOOTH);
        JLabel label = new JLabel(new ImageIcon(image)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                super.paintComponent(g2);
            }
        };
        label.setPreferredSize(new Dimension(s(width), s(height)));
        return label;
    }

    private Font font(int style, int size) {
        return new Font("Segoe UI", style, Math.max(10, s(size)));
    }

    private void showDatabaseEditor() {
        setActiveNav(editDatabaseButton);
        cardLayout.show(contentHost, "databaseEditor");
    }

    private void showAttendanceReport() {
        if (attendanceReportPanel != null) {
            attendanceReportPanel.refreshCurrentClass();
        }
        setActiveNav(attendanceReportNavButton);
        cardLayout.show(contentHost, "attendanceReport");
    }

    private void showClassroomBuilder() {
        contentHost.remove(classroomBuilderPanel);
        boolean sessionStarted = startSessionPanel.getIsSessionStarted();
        String classroomType = sessionStarted ? startSessionPanel.getClassroomType() : null;
        String tName = getLoggedInTeacherName();
        classroomBuilderPanel = new ClassroomBuilder(
                (float) scale,
                db,
                startSessionPanel.getCurrentSession(),
                sessionStarted,
                classroomType,
                tName);
        classroomBuilderPanel.setApp(seatendanceApp);
        contentHost.add(classroomBuilderPanel, "classroomBuilder");
        setActiveNav(classroomBuilderNavButton);
        cardLayout.show(contentHost, "classroomBuilder");
    }

    private void showPermissions() {
        setActiveNav(editStudentPermissionsButton);
        cardLayout.show(contentHost, "permissions");
    }

    private enum IconAsset {
        DASHBOARD("/images/seatendance icons/dashboard.png"),
        START_SESSION("/images/seatendance icons/startSession.png"),
        STUDENTS("/images/seatendance icons/students.png"),
        CLASSROOMS("/images/seatendance icons/classrooms.png"),
        REPORTS("/images/seatendance icons/reports.png"),
        PERMISSION("/images/seatendance icons/permission.png"),
        LOGOUT("/images/seatendance icons/logout.png"),
        ENROLLED("/images/seatendance icons/enrolled.png"),
        PRESENT("/images/seatendance icons/present.png"),
        LATE("/images/seatendance icons/late.png"),
        ABSENT("/images/seatendance icons/absent.png"),
        EXCUSED("/images/seatendance icons/excused.png"),
        FDA("/images/seatendance icons/fda.png"),
        ACTIVE_SESSION("/images/seatendance icons/newactivesession.png"),
        PAST_SESSION("/images/seatendance icons/newpastsession.png");

        private final String path;
        private Image image;

        IconAsset(String path) {
            this.path = path;
        }

        private Image image() {
            if (image == null) {
                java.net.URL resource = Seatendance.class.getResource(path);
                if (resource != null) {
                    image = new ImageIcon(resource).getImage();
                }
            }
            return image;
        }
    }

    private final class NavButton extends JButton {
        private final String label;
        private final IconAsset iconAsset;
        private final Color normalBg;
        private final Color hoverBg;
        private final Timer hoverTimer;
        private boolean hovered;
        private boolean active;
        private double hoverProgress;

        private NavButton(String label, IconAsset iconAsset, Color normalBg, Color hoverBg) {
            this.label = label;
            this.iconAsset = iconAsset;
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(font(Font.BOLD, 18));

            hoverTimer = new Timer(15, e -> updateHoverAnimation());
            hoverTimer.setCoalesce(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    hoverTimer.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    hoverTimer.start();
                }
            });
        }

        private void updateHoverAnimation() {
            double target = hovered && isEnabled() ? 1.0 : 0.0;
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

        private void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double progress = isEnabled() ? hoverProgress : 0.0;
            Color baseBg = active ? RED : normalBg;
            Color targetBg = active ? new Color(204, 49, 58) : hoverBg;
            int inset = (int) Math.round(s(3) * (1.0 - progress));
            int buttonX = inset;
            int buttonY = inset;
            int buttonWidth = getWidth() - inset * 2;
            int buttonHeight = getHeight() - inset * 2;

            g2.setColor(blend(baseBg, targetBg, progress));
            g2.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, s(8), s(8));

            int iconSize = s(34) + (int) Math.round(s(2) * progress);
            paintIconImage(g2, iconAsset, this, buttonX + s(18), buttonY + (buttonHeight - iconSize) / 2, iconSize);

            g2.setFont(getFont().deriveFont((float) (getFont().getSize2D() + Math.max(1, s(1)) * progress)));
            g2.setColor(isEnabled() ? TEXT : MUTED_TEXT);
            FontMetrics fm = g2.getFontMetrics();
            int textY = buttonY + (buttonHeight - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, buttonX + s(74), textY);
            g2.dispose();
        }

        private Color blend(Color from, Color to, double progress) {
            double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
            int red = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * clampedProgress);
            int green = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clampedProgress);
            int blue = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clampedProgress);
            return new Color(red, green, blue);
        }
    }

    private final class IconCircle extends JComponent {
        private final IconAsset iconAsset;
        private final Color background;
        private final int diameter;
        private final int iconSize;

        private IconCircle(IconAsset iconAsset, Color background, int diameter, int iconSize) {
            this.iconAsset = iconAsset;
            this.background = background;
            this.diameter = diameter;
            this.iconSize = iconSize;
            setPreferredSize(new Dimension(s(diameter), s(diameter)));
            setMinimumSize(new Dimension(s(diameter), s(diameter)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int scaledIconSize = s(iconSize);
            paintIconImage(g2, iconAsset, this, (getWidth() - scaledIconSize) / 2,
                    (getHeight() - scaledIconSize) / 2, scaledIconSize);
            g2.dispose();
        }
    }

    private void paintIconImage(Graphics2D g2, IconAsset iconAsset, Component observer, int x, int y, int size) {
        Image image = iconAsset.image();
        if (image == null) {
            return;
        }

        Image scaled = new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH)).getImage();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(scaled, x, y, observer);
    }

    private final class RoundedPanel extends JPanel {
        private final Color fillColor;
        private final Color borderColor;
        private final int radius;
        private final int borderWidth;

        private RoundedPanel(Color fillColor, Color borderColor, int radius, int borderWidth) {
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.radius = radius;
            this.borderWidth = borderWidth;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(radius), s(radius));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            if (borderColor == null || borderWidth <= 0) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(s(borderWidth)));
            int inset = Math.max(1, s(borderWidth));
            g2.drawRoundRect(inset / 2, inset / 2, getWidth() - inset, getHeight() - inset, s(radius), s(radius));
            g2.dispose();
        }
    }

    private final class AccentStrip extends JComponent {
        private final Color color;
        private final int radius;

        private AccentStrip(Color color, int radius) {
            this.color = color;
            this.radius = radius;
            Dimension size = new Dimension(s(8), 0);
            setPreferredSize(size);
            setMinimumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth() + s(radius), getHeight(), s(radius), s(radius));
            g2.dispose();
        }
    }

    public String getLoggedInTeacherName() {
        if ("teacher".equals(role)) {
            return db.getUserName(currentUserEmail);
        }
        return null;
    }

    public void refreshClassroomBuilder() {
        if (classroomBuilderPanel != null) {
            classroomBuilderPanel.refreshClassroom();
        }
    }

    public boolean resetSeats() {
        if (startSessionPanel != null && startSessionPanel.resetSeats()) {
            if (classroomBuilderPanel != null) {
                classroomBuilderPanel.refreshClassroom();
            }
            return true;
        }
        return false;
    }
}