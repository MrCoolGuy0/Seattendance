package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;

import java.awt.*;
import java.awt.event.*;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StartSessionPanel extends ScaledPanel {

    static final Color BG = new Color(34, 38, 42);
    static final Color CARD_BG = new Color(20, 25, 28);
    static final Color CARD_HOVER_BG = new Color(23, 29, 33);
    static final Color CARD_PRESSED_BG = new Color(17, 21, 24);
    static final Color PANEL_BG = new Color(22, 28, 32);
    static final Color CONTROL_BG = new Color(25, 31, 36);
    static final Color CONTROL_HOVER_BG = new Color(33, 40, 47);
    static final Color DISABLED_CONTROL_BG = new Color(42, 47, 52);
    static final Color DISABLED_BORDER = new Color(55, 61, 68);
    static final Color TEXT_SELECTION_BG = new Color(75, 93, 109);
    static final Color BORDER = new Color(63, 70, 78);
    static final Color TEXT = Color.WHITE;
    static final Color MUTED = new Color(174, 182, 193);
    static final Color META = new Color(137, 148, 160);
    static final Color GREEN = new Color(96, 186, 136);
    static final Color YELLOW = new Color(224, 186, 82);
    static final Color RED = new Color(216, 102, 108);
    static final Color BLUE = new Color(102, 156, 218);
    static final Color GRAY = new Color(132, 143, 153);
    static final Color ORANGE = new Color(222, 148, 84);
    static final Color BRAND_RED = new Color(210, 60, 69);
    static final Color CAL_SELECTED_BG = new Color(102, 44, 52);
    static final Color CAL_SELECTED_BORDER = new Color(210, 60, 69);
    static final Color CAL_CONCLUDED_BG = new Color(76, 50, 56);
    static final Color CAL_CONCLUDED_BORDER = new Color(153, 71, 81);
    static final Color CAL_DRAFT_BG = new Color(50, 53, 57);
    static final Color CAL_DRAFT_BORDER = new Color(125, 94, 100);
    static final Color CAL_EMPTY_BG = new Color(34, 37, 40);
    static final Color CAL_EMPTY_BORDER = new Color(67, 73, 79);
    static final Color FOOTER_BG = new Color(17, 22, 25);
    static final Color CALENDAR_BORDER = new Color(45, 54, 62);
    static final Color BADGE_BG = new Color(24, 29, 33);
    static final Color BADGE_BORDER = new Color(48, 56, 63);
    static final int PANEL_RADIUS = 12;
    static final int CONTROL_RADIUS = 10;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final int[] LATE_INTERVALS = { 0, 5, 10, 15, 20, 30 };
    private static final int[] ABSENT_INTERVALS = { 30, 45, 60, 90, 120 };
    private static final String[] ROOM_TYPES = {
            "Computer Laboratory", "Laboratory", "Regular Classroom"
    };

    private final DatabaseHandler db;
    private final MainSeatendanceApp app;
    private final ArrayList<AttendanceReport.ClassOption> classOptions;
    private final String loggedInEmail;
    private final ArrayList<StudentRecord> students = new ArrayList<>();
    private final ArrayList<StudentCard> studentCards = new ArrayList<>();
    private final ArrayList<CourseCode> courseCodes = new ArrayList<>();

    private AttendanceReport.ClassOption selectedClass;
    LocalDate selectedDate;
    private JComboBox<AttendanceReport.ClassOption> classCombo;
    private TimeTextField startTimeField;
    private TimeTextField endTimeField;
    private MinuteTextField lateIntervalField;
    private MinuteTextField absentIntervalField;
    private HintTextField searchField;
    private JComboBox<String> statusFilter;
    private JComboBox<String> classroomTypeCombo;
    JPanel gridPanel;
    private JLabel instructorValueLabel;
    private JLabel studentCountLabel;
    private JLabel unmarkedCountLabel;
    private JLabel autosaveLabel;
    private JLabel dateValueLabel;
    private JButton concludeButton;
    private boolean concluded;
    private boolean updatingTimeControl;
    private final Image profileImage;

    private boolean isSessionStarted = false;
    private boolean classroomTypeLocked = false;
    private String lockedClassroomTypeValue = null;
    private boolean updatingComboFromCode = false;

    // NEW: classroom frame field
    private ScaledFrame classroomFrame;

    // Builds the Start Session screen and connects it to the database and scanner.
    public StartSessionPanel(MainSeatendanceApp app, double scale, List<AttendanceReport.ClassOption> classOptions,
            String loggedInEmail) {
        super(scale);
        this.app = app;
        this.db = app.getDatabase();
        this.classOptions = new ArrayList<>();
        this.loggedInEmail = loggedInEmail;
        this.profileImage = loadImage("/images/seatendance icons/profileicon.png");
        loadCourseCodes();
        for (AttendanceReport.ClassOption option : classOptions) {
            this.classOptions.add(makeClassOption(option));
        }

        setOpaque(true);
        setBackground(BG);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(0, s(18)));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(s(26), s(28), s(18), s(28)));
        root.add(buildTop(), BorderLayout.NORTH);
        root.add(buildGridPanel(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    invalidateAll(StartSessionPanel.this);
                    revalidate();
                    repaint();
                    refreshVisibleCards();
                });
            }
        });

        if (!this.classOptions.isEmpty()) {
            selectClass(this.classOptions.get(0));
        } else {
            setEmptyState("No classes assigned yet.");
        }
        RFIDManager rfidManager = this.app.getRFIDManager();
        if (rfidManager != null) {
            rfidManager.setScanListener(uid -> handleRFIDScan(uid));
        }
    }

    private void invalidateAll(Component component) {
        component.invalidate();
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                invalidateAll(child);
            }
        }
    }

    private void handleRFIDScan(String uid) {
        String studentId = db.getStudentIdByUID(uid);

        SwingUtilities.invokeLater(() -> {
            if (selectedDate == null || selectedClass == null) {
                return;
            }
            LocalTime scannedAt = currentMinute();
            if (concluded) {
                setAutosave("RFID ignored: concluded session is manual edit only", ORANGE);
                return;
            }
            if (!validateClassWindow(scannedAt, "take attendance")) {
                return;
            }

            if (studentId == null) {
                System.out.println("Unknown RFID tapped: " + uid);
                return;
            }

            for (StudentRecord student : students) {
                if (student.studentId.equals(studentId)) {
                    saveRfidScanForStudent(student, scannedAt);
                    return;
                }
            }
        });
    }

    private void saveRfidScanForStudent(StudentRecord student, LocalTime scannedAt) {
        if (student.status == Status.ABSENT || student.status == Status.EXCUSED) {
            setAutosave("RFID ignored: student is " + student.status.label, ORANGE);
            return;
        }

        Status oldStatus = student.status;
        LocalTime oldTimeIn = student.timeIn;
        LocalTime oldTimeOut = student.timeOut;
        String savedMessage;
        String action;

        if ((student.status == Status.PRESENT || student.status == Status.LATE) && student.timeIn != null) {
            if (student.timeOut != null) {
                setAutosave("RFID ignored: Time Out already set", ORANGE);
                return;
            }
            if (scannedAt.isBefore(student.timeIn)) {
                setAutosave("RFID ignored: Time Out cannot be before Time In", ORANGE);
                return;
            }
            student.timeOut = scannedAt;
            savedMessage = "Autosave: Time Out saved";
            action = "Time Out";
        } else {
            Status scannedStatus = statusForTimeIn(scannedAt);
            student.status = scannedStatus;
            student.timeOut = null;
            action = "Time In";
            if (scannedStatus == Status.ABSENT) {
                student.timeIn = null;
                savedMessage = "Autosave: Marked absent";
            } else {
                student.timeIn = scannedAt;
                savedMessage = "Autosave: Time In saved";

                if (student.seatNumber <= 0 && (student.status == Status.PRESENT || student.status == Status.LATE)) {
                    String cType = getClassroomType();
                    student.seatNumber = db.getRandomAvailableSeat(selectedClass.sectionSubjectId, cType);
                    if (student.seatNumber > 0) {
                        db.assignSeat(student.studentId, selectedClass.sectionSubjectId, cType, student.seatNumber);
                        lockClassroomType();
                    }
                    savedMessage = "Autosave: Assigned Seat #" + student.seatNumber + " (" + cType + ")";
                }
            }
        }
        adjustAbsenceCount(student, oldStatus, student.status);

        StudentCard card = findStudentCard(student.studentId);
        refreshStudentCard(card);

        updateCounts(gridPanel.getComponentCount());
        setAutosave("Saving...", YELLOW);

        if (persistStatus(student)) {
            setAutosave(savedMessage, GREEN);
            if (app.getSeatendance() != null) {
                app.getSeatendance().refreshClassroomBuilder();
                refreshClassroomFrame();
                String subjectInfo = selectedClass.subjectCode + " \u2013 " + selectedClass.sectionName;
                LocalTime displayTime = "Time Out".equals(action) ? student.timeOut : student.timeIn;
                app.getSeatendance().pushRecentActivity(
                        student.name,
                        subjectInfo,
                        student.status.label,
                        action,
                        formatTime(displayTime));
            }
        } else {
            adjustAbsenceCount(student, student.status, oldStatus);
            restoreStudentState(student, oldStatus, oldTimeIn, oldTimeOut);
            refreshStudentCard(card);
            setAutosave("Autosave failed", RED);
        }
        refreshVisibleCards();
    }

    public String getCurrentSession() {
        if (selectedClass == null) {
            return null;
        }
        return selectedClass.sectionSubjectId + "_" + (selectedDate != null ? selectedDate.toString() : "none");
    }

    public boolean resetSeats() {
        if (selectedClass == null)
            return false;
        String cType = getClassroomType();
        boolean ok = db.resetSeats(selectedClass.sectionSubjectId, cType);
        if (ok) {
            for (StudentRecord s : students) {
                s.seatNumber = 0;
            }
        }
        return ok;
    }

    public String getClassroomType() {
        if (classroomTypeCombo == null) {
            return "Computer Laboratory";
        }
        return (String) classroomTypeCombo.getSelectedItem();
    }

    public boolean getIsSessionStarted() {
        return isSessionStarted;
    }

    private JPanel buildTop() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Start Attendance Session");
        title.setForeground(TEXT);
        title.setFont(font(Font.BOLD, 34));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Take attendance using the seating grid below.");
        subtitle.setForeground(MUTED);
        subtitle.setFont(font(Font.PLAIN, 16));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(title);
        top.add(Box.createVerticalStrut(s(4)));
        top.add(subtitle);
        top.add(Box.createVerticalStrut(s(18)));
        top.add(buildSessionInfoRow());
        top.add(Box.createVerticalStrut(s(12)));
        top.add(alignedToInfoRow(buildFilters()));
        return top;
    }

    private JPanel buildSessionInfoRow() {
        JPanel row = new JPanel();
        row.setLayout(new WrapLayout(FlowLayout.LEFT, s(12), s(6)));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        FontMetrics metrics15 = getFontMetrics(new Font("Segoe UI", Font.PLAIN, 15));
        int instructorCardWidth = getInstructorCardWidth(metrics15);
        int dateCardWidth = getDateCardWidth(metrics15);
        int roomTypeCardWidth = getRoomTypeCardWidth();

        instructorValueLabel = valueLabel("Select a class");
        row.add(infoCard("Instructor", instructorValueLabel, instructorCardWidth));

        classCombo = buildClassCombo();
        row.add(infoCard("Class", classCombo, getClassComboWidth()));

        dateValueLabel = valueLabel("-------");
        JPanel dateCard = infoCard("Session Date", dateValueLabel, dateCardWidth);
        attachDatePicker(dateCard);
        row.add(dateCard);

        startTimeField = sessionTimeField("", 105);
        row.add(infoCard("Class Starts", startTimeField, 128));

        endTimeField = sessionTimeField("", 105);
        row.add(infoCard("Class Ends", endTimeField, 128));

        lateIntervalField = minuteField(15, 90);
        row.add(infoCard("Late After", lateIntervalField, 115));

        absentIntervalField = minuteField(45, 90);
        row.add(infoCard("Absent After", absentIntervalField, 115));

        classroomTypeCombo = buildClassroomTypeCombo();
        row.add(infoCard("Classroom Type", classroomTypeCombo, roomTypeCardWidth));
        updateSessionControlsEnabled();
        return row;
    }

    private int getInstructorCardWidth(FontMetrics metrics) {
        int maxInstructorWidth = metrics.stringWidth("Select a class");
        for (AttendanceReport.ClassOption option : classOptions) {
            maxInstructorWidth = Math.max(maxInstructorWidth, metrics.stringWidth(teacherNameOrUnknown(option)));
        }
        return Math.max(150, maxInstructorWidth + 36);
    }

    private int getDateCardWidth(FontMetrics metrics) {
        int maxDateWidth = metrics.stringWidth("September 30, 2026 (Wednesday)");
        return Math.max(200, maxDateWidth + 36);
    }

    private int getRoomTypeCardWidth() {
        FontMetrics metrics = getFontMetrics(new Font("Segoe UI", Font.PLAIN, 14));
        int maxRoomTypeWidth = 0;
        for (String roomType : ROOM_TYPES) {
            maxRoomTypeWidth = Math.max(maxRoomTypeWidth, metrics.stringWidth(roomType));
        }
        return Math.max(200, maxRoomTypeWidth + 100);
    }

    private StyledComboBox<AttendanceReport.ClassOption> buildClassCombo() {
        StyledComboBox<AttendanceReport.ClassOption> combo = new StyledComboBox<>(
                classOptions.toArray(new AttendanceReport.ClassOption[0]));
        combo.setFont(font(Font.PLAIN, 16));
        combo.setForeground(TEXT);
        combo.setFocusable(false);
        combo.setEnabled(!classOptions.isEmpty());
        combo.setPreferredSize(new Dimension(0, s(38)));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AttendanceReport.ClassOption) {
                    AttendanceReport.ClassOption option = (AttendanceReport.ClassOption) value;
                    setText(getClassDisplayText(option));
                }
                styleComboListCell(this, isSelected);
                setOpaque(index >= 0);
                return this;
            }
        });
        combo.addActionListener(e -> {
            Object item = combo.getSelectedItem();
            if (item instanceof AttendanceReport.ClassOption) {
                AttendanceReport.ClassOption option = (AttendanceReport.ClassOption) item;
                selectClass(option);
            }
        });
        return combo;
    }

    private StyledComboBox<String> buildClassroomTypeCombo() {
        StyledComboBox<String> combo = new StyledComboBox<>(ROOM_TYPES);
        combo.setFont(font(Font.PLAIN, 14));
        combo.setForeground(TEXT);
        combo.setFocusable(false);
        combo.setPreferredSize(new Dimension(0, s(38)));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                styleComboListCell(this, isSelected);
                setOpaque(index >= 0);
                return this;
            }
        });
        combo.addItemListener(e -> {
            if (updatingComboFromCode)
                return;
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                if (classroomTypeLocked && lockedClassroomTypeValue != null
                        && !lockedClassroomTypeValue.equals(combo.getSelectedItem())) {
                    updatingComboFromCode = true;
                    combo.setSelectedItem(lockedClassroomTypeValue);
                    updatingComboFromCode = false;
                } else if (!classroomTypeLocked) {
                    saveSessionTimesToDb();
                    refreshClassroomFrame();
                }
            }
        });
        return combo;
    }

    private void styleComboListCell(JLabel label, boolean isSelected) {
        label.setBorder(new EmptyBorder(s(8), s(12), s(8), s(12)));
        if (isSelected) {
            label.setBackground(CONTROL_HOVER_BG);
        } else {
            label.setBackground(CONTROL_BG);
        }
        label.setForeground(TEXT);
    }

    private TimeTextField sessionTimeField(String selected, int width) {
        TimeTextField field = new TimeTextField(selected);
        Dimension size = new Dimension(s(width), s(36));
        setFixedSize(field, size);
        return field;
    }

    private MinuteTextField minuteField(int defaultMinutes, int width) {
        MinuteTextField field = new MinuteTextField(defaultMinutes);
        Dimension size = new Dimension(s(width), s(36));
        setFixedSize(field, size);
        return field;
    }

    private StyledComboBox<String> sessionCombo(String[] values, String selected, int width) {
        StyledComboBox<String> combo = new StyledComboBox<>(values);
        Dimension size = new Dimension(s(width), s(36));
        setFixedSize(combo, size);
        combo.setFont(font(Font.PLAIN, 14));
        combo.setFocusable(false);
        combo.setSelectedItem(selected);
        combo.putClientProperty("lastValidTime", parseTime(selected));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                boolean enabled = !combo.isSoftDisabled();
                Color background;
                if (!enabled) {
                    background = DISABLED_CONTROL_BG;
                } else if (isSelected) {
                    background = CONTROL_HOVER_BG;
                } else {
                    background = CONTROL_BG;
                }

                setOpaque(index != -1);
                setText(String.valueOf(value));
                setBackground(background);
                if (enabled) {
                    setForeground(TEXT);
                } else {
                    setForeground(META);
                }
                setBorder(new EmptyBorder(s(8), s(10), s(8), s(10)));
                return this;
            }
        });
        combo.addActionListener(e -> refreshTimeBasedStatuses());
        return combo;
    }

    private String[] intervalOptions(int[] minutes) {
        String[] values = new String[minutes.length];
        for (int i = 0; i < minutes.length; i++) {
            values[i] = minutes[i] + " min";
        }
        return values;
    }

    private void loadCourseCodes() {
        courseCodes.clear();
        try (ResultSet rs = db.getCourses()) {
            if (rs == null) {
                return;
            }
            while (rs.next()) {
                CourseCode code = new CourseCode(
                        safe(rs.getString("course")),
                        safe(rs.getString("course_code")));
                courseCodes.add(code);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getClassDisplayText(AttendanceReport.ClassOption option) {
        return getShortSectionName(option.sectionName) + "  -  " + option.subjectCode;
    }

    private String teacherNameOrUnknown(AttendanceReport.ClassOption option) {
        if (option == null || option.teacherName == null || option.teacherName.isEmpty()) {
            return "(unknown)";
        }
        return option.teacherName;
    }

    private int getClassComboWidth() {
        FontMetrics metrics = getFontMetrics(new Font("Segoe UI", Font.PLAIN, 16));
        int longestText = 0;
        for (AttendanceReport.ClassOption option : classOptions) {
            longestText = Math.max(longestText, metrics.stringWidth(getClassDisplayText(option)));
        }
        return Math.max(220, Math.min(560, longestText + 100));
    }

    private AttendanceReport.ClassOption makeClassOption(AttendanceReport.ClassOption option) {
        return new AttendanceReport.ClassOption(
                option.sectionSubjectId,
                getShortSectionName(option.sectionName),
                option.subjectCode,
                option.subjectName,
                option.teacherName);
    }

    private String getShortSectionName(String sectionName) {
        String safeSectionName = safe(sectionName);
        String bestCourse = "";
        String bestCode = "";

        for (CourseCode courseCode : courseCodes) {
            if (courseCode.courseName.isEmpty() || courseCode.courseCode.isEmpty()) {
                continue;
            }
            if ((safeSectionName.equals(courseCode.courseName)
                    || safeSectionName.startsWith(courseCode.courseName + " "))
                    && courseCode.courseName.length() > bestCourse.length()) {
                bestCourse = courseCode.courseName;
                bestCode = courseCode.courseCode;
            }
        }

        if (bestCourse.isEmpty()) {
            return safeSectionName;
        }
        return bestCode + safeSectionName.substring(bestCourse.length());
    }

    void setFixedSize(Component component, Dimension size) {
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
    }

    private JPanel alignedToInfoRow(JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, s(12), 0, 0));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private void attachDatePicker(Component component) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                changeDate();
            }
        });

        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                attachDatePicker(child);
            }
        }
    }

    private JPanel infoCard(String title, JComponent value, int width) {
        RoundedPanel card = new RoundedPanel(PANEL_BG, null, PANEL_RADIUS, 0);
        card.setLayout(new BorderLayout(0, s(9)));
        card.setPreferredSize(new Dimension(s(width), s(78)));
        card.setMinimumSize(new Dimension(s(150), s(78)));
        card.setBorder(new EmptyBorder(s(9), s(12), s(10), s(12)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(font(Font.PLAIN, 13));
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        if (value instanceof JComboBox<?>) {
            gbc.fill = GridBagConstraints.HORIZONTAL;
        } else {
            gbc.fill = GridBagConstraints.NONE;
        }
        gbc.anchor = GridBagConstraints.WEST;
        row.add(value, gbc);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JLabel valueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(font(Font.PLAIN, 15));
        return label;
    }

    private JPanel buildFilters() {
        RoundedPanel filters = new RoundedPanel(PANEL_BG, null, PANEL_RADIUS, 0);
        filters.setLayout(new BorderLayout(0, 0));
        filters.setBorder(new EmptyBorder(s(10), s(12), s(10), s(12)));
        Dimension filterSize = new Dimension(0, s(60));
        filters.setPreferredSize(filterSize);
        filters.setMinimumSize(filterSize);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(60)));
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setOpaque(false);

        searchField = new HintTextField("Search student...");
        Dimension searchSize = new Dimension(s(230), s(36));
        setFixedSize(searchField, searchSize);
        searchField.setAlignmentY(Component.CENTER_ALIGNMENT);
        searchField.setFont(font(Font.PLAIN, 14));
        addSearchListener(searchField);
        controls.add(searchField);
        controls.add(Box.createHorizontalStrut(s(10)));

        statusFilter = new StyledComboBox<>(new String[] {
                "All Statuses", "Unmarked", "Present", "Late", "Absent", "Excused", "Warning", "FDA"
        });
        Dimension filterComboSize = new Dimension(s(160), s(36));
        setFixedSize(statusFilter, filterComboSize);
        statusFilter.setAlignmentY(Component.CENTER_ALIGNMENT);
        statusFilter.setFont(font(Font.PLAIN, 14));
        statusFilter.setFocusable(false);
        statusFilter.addActionListener(e -> refreshVisibleCards());
        controls.add(statusFilter);
        controls.add(Box.createHorizontalStrut(s(10)));

        JPanel legendRow = new JPanel(new FlowLayout(FlowLayout.LEFT, s(6), 0));
        legendRow.setOpaque(false);
        legendRow.setAlignmentY(Component.CENTER_ALIGNMENT);
        legendRow.add(legendItem("Unmarked", GRAY));
        legendRow.add(legendItem("Present", GREEN));
        legendRow.add(legendItem("Late", YELLOW));
        legendRow.add(legendItem("Absent", RED));
        legendRow.add(legendItem("Excused", BLUE));
        legendRow.add(legendItem("Warning", ORANGE));
        legendRow.add(legendItem("FDA", BRAND_RED));
        Dimension legendSize = legendRow.getPreferredSize();
        legendRow.setPreferredSize(legendSize);
        legendRow.setMinimumSize(legendSize);
        legendRow.setMaximumSize(new Dimension(legendSize.width, searchSize.height));
        controls.add(legendRow);

        studentCountLabel = new JLabel("0 students");
        studentCountLabel.setForeground(MUTED);
        studentCountLabel.setFont(font(Font.BOLD, 13));
        studentCountLabel.setBorder(new EmptyBorder(0, s(10), 0, 0));

        JPanel countPanel = new JPanel(new GridBagLayout());
        countPanel.setOpaque(false);
        countPanel.add(studentCountLabel);

        filters.add(controls, BorderLayout.CENTER);
        filters.add(countPanel, BorderLayout.EAST);
        return filters;
    }

    private void addSearchListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshVisibleCards();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshVisibleCards();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshVisibleCards();
            }
        });
    }

    private JPanel legendItem(String label, Color color) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, s(5), 0));
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(s(5), 0, s(4), 0));
        item.setAlignmentY(Component.CENTER_ALIGNMENT);
        item.add(new StatusDot(color, 8));
        JLabel text = new JLabel(label);
        text.setForeground(MUTED);
        text.setFont(font(Font.PLAIN, 12));
        item.add(text);
        Dimension itemSize = item.getPreferredSize();
        item.setPreferredSize(itemSize);
        item.setMinimumSize(itemSize);
        item.setMaximumSize(itemSize);
        return item;
    }

    private JScrollPane buildGridPanel() {
        gridPanel = new ScrollableGridPanel(new GridLayout(0, 4, s(22), s(20)));
        gridPanel.setOpaque(false);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setViewportBorder(new EmptyBorder(s(18), s(8), s(8), s(26)));
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        styleScrollBar(scroll.getVerticalScrollBar());
        return scroll;
    }

    private JPanel buildFooter() {
        RoundedPanel footer = new RoundedPanel(FOOTER_BG, null, PANEL_RADIUS, 0);
        footer.setLayout(new GridBagLayout());
        footer.setBorder(new EmptyBorder(s(7), s(14), s(7), s(14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, s(11), s(6)));
        left.setOpaque(false);
        left.add(new StatusDot(GRAY, 8));
        unmarkedCountLabel = new JLabel("Unmarked: 0");
        unmarkedCountLabel.setForeground(TEXT);
        unmarkedCountLabel.setFont(font(Font.PLAIN, 14));
        left.add(unmarkedCountLabel);
        left.add(Box.createHorizontalStrut(s(12)));
        left.add(new StatusDot(GREEN, 12));
        autosaveLabel = new JLabel("Autosave: All changes saved");
        autosaveLabel.setForeground(TEXT);
        autosaveLabel.setFont(font(Font.PLAIN, 14));
        left.add(autosaveLabel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, s(12), 0));
        actions.setOpaque(false);
        concludeButton = actionButton("Conclude Session", BRAND_RED, new Color(222, 55, 64), TEXT);
        concludeButton.addActionListener(e -> concludeSession());
        actions.add(concludeButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        footer.add(left, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        footer.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        footer.add(actions, gbc);
        return footer;
    }

    private JButton actionButton(String text, Color bg, Color border, Color fg) {
        JButton button = new ActionButton(text, bg, border, fg);
        Dimension size = new Dimension(s(190), s(40));
        setFixedSize(button, size);
        button.setFont(font(Font.BOLD, 14));
        return button;
    }

    private void selectClass(AttendanceReport.ClassOption option) {
        if (option == null)
            return;

        saveSessionTimesToDb();
        selectedClass = option;
        concluded = false;
        updateDateAndSessionLabels();

        if (classCombo != null && classCombo.getSelectedItem() != option)
            classCombo.setSelectedItem(option);
        instructorValueLabel.setText(teacherNameOrUnknown(option));

        loadStudents(option.sectionSubjectId);
        if (isSessionStarted) {
            showClassroomFrame();
        }

        if (selectedDate == null)
            setAutosave("Select a date to begin", GREEN);
        else
            setAutosave("Autosave: All changes saved", GREEN);
    }

    private void changeDate() {
        if (selectedClass == null)
            return;

        Window owner = SwingUtilities.getWindowAncestor(this);
        CalendarPicker picker = new CalendarPicker(owner, selectedDate);
        LocalDate chosen = picker.showDialog();
        if (chosen == null)
            return;

        saveSessionTimesToDb();
        selectedDate = chosen;
        concluded = false;
        updateDateAndSessionLabels(); // sets isSessionStarted
        loadStudents(selectedClass.sectionSubjectId);
        if (isSessionStarted) { // ← only show after students are loaded
            showClassroomFrame();
        }
        setAutosave("Autosave: All changes saved", GREEN);
    }

    private Date selectedSqlDate() {
        if (selectedDate == null) {
            return null;
        }
        return Date.valueOf(selectedDate);
    }

    // Updates the date label and checks if the selected session is concluded.
    private void updateDateAndSessionLabels() {
        if (dateValueLabel == null) {
            return;
        }

        if (selectedDate == null || selectedClass == null) {
            concluded = false;
            isSessionStarted = false;
            dateValueLabel.setText("-------");
            if (concludeButton != null) {
                concludeButton.setEnabled(false);
            }
            hideClassroomFrame();
            updateSessionControlsEnabled();
            return;
        }

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy (EEEE)", Locale.ENGLISH);
        dateValueLabel.setText(selectedDate.format(dateFormat));
        String status = db.getAttendanceSessionStatus(selectedClass.sectionSubjectId, selectedSqlDate());
        concluded = "concluded".equalsIgnoreCase(status);
        isSessionStarted = !concluded;
        if (concludeButton != null) {
            concludeButton.setEnabled(!concluded);
        }

        if (!isSessionStarted) {
            hideClassroomFrame();
        }

        if (startTimeField != null)
            startTimeField.clearTimeValue();
        if (endTimeField != null)
            endTimeField.clearTimeValue();
        classroomTypeLocked = false;
        lockedClassroomTypeValue = null;
        if (classroomTypeCombo instanceof StyledComboBox<?>) {
            ((StyledComboBox<?>) classroomTypeCombo).setSoftDisabled(false);
        }

        String[] times = db.getSessionTimes(selectedClass.sectionSubjectId, selectedSqlDate());
        System.out.println("[DEBUG] Loading times for ssid=" + selectedClass.sectionSubjectId
                + " date=" + selectedSqlDate() + " -> "
                + (times == null ? "null" : "[" + times[0] + ", " + times[1] + ", " + times[2] + "]"));
        if (times != null) {
            if (times[0] != null && !times[0].isEmpty() && startTimeField != null) {
                LocalTime parsed = parseTypedTime(times[0]);
                if (parsed != null)
                    startTimeField.restoreTimeValue(parsed);
            }
            if (times[1] != null && !times[1].isEmpty() && endTimeField != null) {
                LocalTime parsed = parseTypedTime(times[1]);
                if (parsed != null)
                    endTimeField.restoreTimeValue(parsed);
            }
            if (times[2] != null && !times[2].isEmpty() && classroomTypeCombo != null) {
                updatingComboFromCode = true;
                classroomTypeCombo.setSelectedItem(times[2]);
                updatingComboFromCode = false;
            }
            boolean hasAttendance = db.hasAnyAttendance(selectedClass.sectionSubjectId, selectedSqlDate());
            if (concluded || hasAttendance) {
                classroomTypeLocked = true;
                lockedClassroomTypeValue = getClassroomType();
                if (classroomTypeCombo instanceof StyledComboBox<?>) {
                    ((StyledComboBox<?>) classroomTypeCombo).setSoftDisabled(true);
                }
            }
        }

        updateSessionControlsEnabled();
    }

    private void updateSessionControlsEnabled() {
        boolean enabled = selectedDate != null && selectedClass != null;
        setSessionControlEnabled(startTimeField, enabled);
        setSessionControlEnabled(endTimeField, enabled);
        setSessionControlEnabled(lateIntervalField, enabled);
        setSessionControlEnabled(absentIntervalField, enabled);
        if (!classroomTypeLocked && classroomTypeCombo instanceof StyledComboBox<?>) {
            ((StyledComboBox<?>) classroomTypeCombo).setSoftDisabled(false);
        }
    }

    private void updateClassroomTypeLock() {
        if (concluded)
            return;
        boolean anyMarked = false;
        for (StudentRecord sr : students) {
            if (sr.status != null && sr.status != Status.UNMARKED) {
                anyMarked = true;
                break;
            }
        }
        if (anyMarked && !classroomTypeLocked) {
            classroomTypeLocked = true;
            lockedClassroomTypeValue = getClassroomType();
            if (classroomTypeCombo instanceof StyledComboBox<?>) {
                ((StyledComboBox<?>) classroomTypeCombo).setSoftDisabled(true);
            }
        } else if (!anyMarked && classroomTypeLocked && !concluded) {
            classroomTypeLocked = false;
            lockedClassroomTypeValue = null;
            if (classroomTypeCombo instanceof StyledComboBox<?>) {
                ((StyledComboBox<?>) classroomTypeCombo).setSoftDisabled(false);
            }
        }
    }

    private void lockClassroomType() {
        updateClassroomTypeLock();
    }

    private void setSessionControlEnabled(JComponent component, boolean enabled) {
        if (component instanceof StyledComboBox<?>) {
            StyledComboBox<?> styledCombo = (StyledComboBox<?>) component;
            styledCombo.setSoftDisabled(!enabled);
        } else if (component instanceof TimeTextField) {
            TimeTextField timeField = (TimeTextField) component;
            timeField.setSoftDisabled(!enabled);
        } else if (component instanceof MinuteTextField) {
            MinuteTextField minuteField = (MinuteTextField) component;
            minuteField.setSoftDisabled(!enabled);
        } else if (component != null) {
            component.setEnabled(enabled);
        }
    }

    private void refreshTimeBasedStatuses() {
        if (selectedDate == null || selectedClass == null || students.isEmpty() || !isClassWindowValid()) {
            return;
        }

        boolean changed = false;
        for (StudentRecord student : students) {
            if (student.timeIn == null || student.status == Status.EXCUSED) {
                continue;
            }
            Status oldStatus = student.status;
            Status newStatus = statusForTimeIn(student.timeIn);
            if (oldStatus != newStatus) {
                student.status = newStatus;
                adjustAbsenceCount(student, oldStatus, newStatus);
                changed = true;
                persistStatus(student);
            }
        }
        if (changed) {
            refreshVisibleCards();
            setAutosave("Autosave: All changes saved", GREEN);
        }
    }

    private void loadStudents(int sectionSubjectId) {
        students.clear();
        studentCards.clear();

        ArrayList<AttendanceMarkItem> marks = loadAttendanceMarkItems(sectionSubjectId);
        ArrayList<AbsenceCountItem> absenceCounts = loadAbsenceCountItems(sectionSubjectId);

        try (ResultSet rs = db.getEnrolledStudents(sectionSubjectId)) {
            if (rs != null) {
                while (rs.next()) {
                    String studentId = rs.getString("student_id");
                    DatabaseHandler.AttendanceMark mark = findAttendanceMark(marks, studentId);
                    String statusFromDb = null;
                    LocalTime timeIn = null;
                    LocalTime timeOut = null;

                    if (mark != null) {
                        statusFromDb = mark.status;
                        if (mark.timeIn != null) {
                            timeIn = mark.timeIn.toLocalTime();
                        }
                        if (mark.timeOut != null) {
                            timeOut = mark.timeOut.toLocalTime();
                        }
                    }

                    StudentRecord student = new StudentRecord(
                            studentId,
                            formatStudentName(rs),
                            Status.fromDb(statusFromDb),
                            findAbsenceCount(absenceCounts, studentId),
                            timeIn,
                            timeOut,
                            mark != null ? mark.seatNumber : 0);
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        refreshVisibleCards();
    }

    private ArrayList<AttendanceMarkItem> loadAttendanceMarkItems(int sectionSubjectId) {
        ArrayList<AttendanceMarkItem> marks = new ArrayList<>();
        if (selectedDate == null) {
            return marks;
        }

        for (java.util.Map.Entry<String, DatabaseHandler.AttendanceMark> entry : db
                .getAttendanceMarksForDate(sectionSubjectId, selectedSqlDate()).entrySet()) {
            AttendanceMarkItem item = new AttendanceMarkItem(entry.getKey(), entry.getValue());
            marks.add(item);
        }
        return marks;
    }

    private DatabaseHandler.AttendanceMark findAttendanceMark(ArrayList<AttendanceMarkItem> marks, String studentId) {
        for (AttendanceMarkItem item : marks) {
            if (item.studentId.equals(studentId)) {
                return item.mark;
            }
        }
        return null;
    }

    private ArrayList<AbsenceCountItem> loadAbsenceCountItems(int sectionSubjectId) {
        ArrayList<AbsenceCountItem> counts = new ArrayList<>();

        for (java.util.Map.Entry<String, Integer> entry : db.getAbsenceCounts(sectionSubjectId).entrySet()) {
            AbsenceCountItem item = new AbsenceCountItem(entry.getKey(), entry.getValue());
            counts.add(item);
        }
        return counts;
    }

    private int findAbsenceCount(ArrayList<AbsenceCountItem> counts, String studentId) {
        for (AbsenceCountItem item : counts) {
            if (item.studentId.equals(studentId)) {
                return item.count;
            }
        }
        return 0;
    }

    private String formatStudentName(ResultSet rs) throws SQLException {
        String lastName = safe(rs.getString("last_name"));
        String firstName = safe(rs.getString("first_name"));
        String middle = safe(rs.getString("middle_initial"));
        String suffix = "";
        if (!middle.isEmpty()) {
            suffix = " " + middle;
        }
        return (lastName + ", " + firstName + suffix).trim();
    }

    private void setEmptyState(String message) {
        if (instructorValueLabel != null) {
            instructorValueLabel.setText("(none)");
        }
        if (concludeButton != null) {
            concludeButton.setEnabled(false);
        }

        gridPanel.removeAll();
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(MUTED);
        label.setFont(font(Font.PLAIN, 18));
        gridPanel.setLayout(new BorderLayout());
        gridPanel.add(label, BorderLayout.CENTER);
        updateCounts(0);
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void resetBoardAfterConclude() {
        concluded = false;
        classroomTypeLocked = false;
        lockedClassroomTypeValue = null;
        if (classroomTypeCombo instanceof StyledComboBox<?>) {
            ((StyledComboBox<?>) classroomTypeCombo).setSoftDisabled(false);
        }
        for (StudentRecord student : students) {
            student.status = Status.UNMARKED;
            student.timeIn = null;
            student.timeOut = null;
        }
        for (StudentCard card : studentCards) {
            card.refresh();
            card.setEnabled(true);
        }
        concludeButton.setEnabled(true);
        refreshVisibleCards();
    }

    private StudentCard findStudentCard(String studentId) {
        for (StudentCard card : studentCards) {
            if (card.student.studentId.equals(studentId)) {
                return card;
            }
        }
        return null;
    }

    private void refreshStudentCard(StudentCard card) {
        if (card != null) {
            card.refresh();
        }
    }

    private void restoreStudentState(StudentRecord student, Status oldStatus,
            LocalTime oldTimeIn, LocalTime oldTimeOut) {
        student.status = oldStatus;
        student.timeIn = oldTimeIn;
        student.timeOut = oldTimeOut;
    }

    private void refreshVisibleCards() {
        if (gridPanel == null) {
            return;
        }

        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(0, calculateGridColumns(), s(22), s(20)));

        String query = "";
        if (searchField != null) {
            query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        }

        String filter = "All Statuses";
        if (statusFilter != null) {
            filter = String.valueOf(statusFilter.getSelectedItem());
        }
        int visible = 0;

        for (StudentRecord student : students) {
            if (!matches(student, query, filter)) {
                continue;
            }

            StudentCard card = findStudentCard(student.studentId);
            if (card == null) {
                card = new StudentCard(this, student);
                studentCards.add(card);
            }
            card.refresh();
            gridPanel.add(card);
            visible++;
        }

        if (visible == 0) {
            String message;
            if (students.isEmpty()) {
                message = "No students enrolled in this class.";
            } else {
                message = "No matching students.";
            }
            JLabel label = new JLabel(message, SwingConstants.CENTER);
            label.setForeground(MUTED);
            label.setFont(font(Font.PLAIN, 18));
            gridPanel.setLayout(new BorderLayout());
            gridPanel.add(label, BorderLayout.CENTER);
        }

        updateCounts(visible);
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    int calculateGridColumns() {
        int availableWidth = gridPanel.getWidth();
        Container parent = gridPanel.getParent();
        if (parent instanceof JViewport && parent.getWidth() > 0) {
            JViewport viewport = (JViewport) parent;
            availableWidth = viewport.getWidth() - s(28);
        }
        if (availableWidth <= 0) {
            availableWidth = Math.max(s(520), getWidth() - s(56));
        }

        int gap = s(22);
        int preferredCardWidth = s(330);
        int columns = Math.max(2, (availableWidth + gap) / (preferredCardWidth + gap));
        return Math.max(2, Math.min(4, columns));
    }

    private boolean matches(StudentRecord student, String query, String filter) {
        boolean textMatch = query == null || query.isEmpty()
                || student.name.toLowerCase(Locale.ROOT).contains(query)
                || student.studentId.toLowerCase(Locale.ROOT).contains(query);
        if (!textMatch) {
            return false;
        }

        if ("All Statuses".equals(filter)) {
            return true;
        }
        if ("Warning".equals(filter)) {
            return student.isWarning();
        }
        if ("FDA".equals(filter)) {
            return student.isFda();
        }
        return student.status.label.equals(filter);
    }

    private void updateCounts(int visibleCount) {
        int unmarked = 0;
        for (StudentRecord student : students) {
            if (student.status == Status.UNMARKED) {
                unmarked++;
            }
        }

        if (studentCountLabel != null) {
            if (visibleCount == 1) {
                studentCountLabel.setText(visibleCount + " student");
            } else {
                studentCountLabel.setText(visibleCount + " students");
            }
        }
        if (unmarkedCountLabel != null) {
            unmarkedCountLabel.setText("Unmarked: " + unmarked);
        }
    }

    void cycleStudent(StudentRecord student) {
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a date first before taking attendance.",
                    "Select Date",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validateManualAttendanceAction("take attendance")) {
            return;
        }

        Status oldStatus = student.status;
        LocalTime oldTimeIn = student.timeIn;
        LocalTime oldTimeOut = student.timeOut;
        student.status = student.status.next();
        if (student.status == Status.UNMARKED || student.status == Status.ABSENT || student.status == Status.EXCUSED) {
            student.timeIn = null;
            student.timeOut = null;
            if (student.seatNumber > 0) {
                String cType = getClassroomType();
                db.removeSeat(student.studentId, selectedClass.sectionSubjectId, cType);
                student.seatNumber = 0;
            }
            updateClassroomTypeLock();
        } else {
            if (student.seatNumber <= 0) {
                String cType = getClassroomType();
                student.seatNumber = db.getRandomAvailableSeat(selectedClass.sectionSubjectId, cType);
                if (student.seatNumber > 0) {
                    db.assignSeat(student.studentId, selectedClass.sectionSubjectId, cType, student.seatNumber);
                    lockClassroomType();
                }
            }
        }
        adjustAbsenceCount(student, oldStatus, student.status);

        StudentCard card = findStudentCard(student.studentId);
        refreshStudentCard(card);
        updateCounts(gridPanel.getComponentCount());
        setAutosave("Saving...", YELLOW);

        if (persistStatus(student)) {
            setAutosave("Autosave: All changes saved", GREEN);
            if (app.getSeatendance() != null) {
                app.getSeatendance().refreshClassroomBuilder();
                refreshClassroomFrame();
                String subjectInfo = selectedClass.subjectCode + " \u2013 " + selectedClass.sectionName;
                app.getSeatendance().pushRecentActivity(
                        student.name,
                        subjectInfo,
                        student.status.label,
                        "Time In",
                        formatTime(student.timeIn));
            }
        } else {
            adjustAbsenceCount(student, student.status, oldStatus);
            restoreStudentState(student, oldStatus, oldTimeIn, oldTimeOut);
            refreshStudentCard(card);
            setAutosave("Autosave failed", RED);
        }

        refreshVisibleCards();
    }

    private void adjustAbsenceCount(StudentRecord student, Status from, Status to) {
        if (from == Status.ABSENT) {
            student.absenceCount = Math.max(0, student.absenceCount - 1);
        }
        if (to == Status.ABSENT) {
            student.absenceCount++;
        }
    }

    private boolean persistStatus(StudentRecord student) {
        if (selectedClass == null || selectedDate == null) {
            return false;
        }

        Date sqlDate = selectedSqlDate();
        boolean correctionMode = concluded
                || "concluded".equalsIgnoreCase(db.getAttendanceSessionStatus(selectedClass.sectionSubjectId, sqlDate));
        if (!correctionMode && !db.ensureAttendanceSession(selectedClass.sectionSubjectId, sqlDate)) {
            return false;
        }

        boolean success;
        if (student.status == Status.UNMARKED) {
            success = db.clearAttendanceMark(student.studentId, selectedClass.sectionSubjectId, sqlDate);
        } else {
            Time timeIn = null;
            if (student.timeIn != null) {
                LocalTime cleanTimeIn = student.timeIn.withSecond(0).withNano(0);
                timeIn = Time.valueOf(cleanTimeIn);
            }

            Time timeOut = null;
            if (student.timeOut != null) {
                LocalTime cleanTimeOut = student.timeOut.withSecond(0).withNano(0);
                timeOut = Time.valueOf(cleanTimeOut);
            }
            success = db.markAttendance(student.studentId, selectedClass.sectionSubjectId, sqlDate, timeIn, timeOut,
                    student.status.dbValue, student.seatNumber);
        }

        updateDateAndSessionLabels();
        return success;
    }

    private boolean saveAllStatuses() {
        boolean saved = true;
        for (StudentRecord student : students) {
            boolean savedStudent = persistStatus(student);
            if (!savedStudent) {
                saved = false;
            }
        }
        return saved;
    }

    // Finalizes the session and marks remaining unmarked students as Present.
    private void concludeSession() {
        if (selectedDate == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a date first before concluding the session.",
                    "Select Date",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int unmarked = countUnmarkedStudents();
        String message;
        if (unmarked > 0) {
            String studentText;
            if (unmarked == 1) {
                studentText = " student is";
            } else {
                studentText = " students are";
            }
            message = unmarked + studentText
                    + " still unmarked. Concluding will mark them Present. Continue?";
        } else {
            message = "Conclude this attendance session? Current marks will be saved.";
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                "Conclude Session",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (unmarked > 0) {
            markUnmarkedPresent();
        }

        if (!saveAllStatuses()) {
            setAutosave("Conclude failed", RED);
            JOptionPane.showMessageDialog(this, "Some attendance marks could not be saved.", "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String startTimeStr = (startTimeField != null && !startTimeField.getText().trim().isEmpty())
                ? startTimeField.getText().trim()
                : null;
        String endTimeStr = (endTimeField != null && !endTimeField.getText().trim().isEmpty())
                ? endTimeField.getText().trim()
                : null;
        String cType = getClassroomType();
        db.markAttendanceSessionConcluded(selectedClass.sectionSubjectId, selectedSqlDate(),
                startTimeStr, endTimeStr, cType);

        db.assignRemainingSeats(selectedClass.sectionSubjectId, cType);

        concluded = true;
        concludeButton.setEnabled(false);
        for (StudentCard card : studentCards) {
            card.setEnabled(false);
        }
        selectedDate = null;
        isSessionStarted = false;

        // NEW: hide the classroom frame when session concludes
        hideClassroomFrame();

        resetBoardAfterConclude();
        updateDateAndSessionLabels();
        setAutosave("Session concluded. Required to pick a new date.", GREEN);
        JOptionPane.showMessageDialog(this, "Attendance session concluded.", "Session Concluded",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private int countUnmarkedStudents() {
        int count = 0;
        for (StudentRecord student : students) {
            if (student.status == Status.UNMARKED) {
                count++;
            }
        }
        return count;
    }

    private void markUnmarkedPresent() {
        for (StudentRecord student : students) {
            if (student.status == Status.UNMARKED) {
                student.status = Status.PRESENT;
                StudentCard card = findStudentCard(student.studentId);
                refreshStudentCard(card);
            }
        }
        updateCounts(gridPanel.getComponentCount());
        refreshVisibleCards();
    }

    private void setAutosave(String text, Color color) {
        if (autosaveLabel == null) {
            return;
        }
        autosaveLabel.setText(text);
        if (color == GREEN) {
            autosaveLabel.setForeground(TEXT);
        } else {
            autosaveLabel.setForeground(color);
        }
    }

    void handleTimeFieldActivation(StudentRecord student, boolean timeIn, StudentCard.TimeEntryField field) {
        if (selectedDate == null) {
            field.refresh();
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a date first before setting a time.",
                    "Select Date",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (field.hasTimeValue() || concluded) {
            field.beginEdit();
            return;
        }

        if (!timeIn && student.timeIn == null) {
            field.refresh();
            JOptionPane.showMessageDialog(
                    this,
                    "Set Time In first before setting Time Out.",
                    "Missing Time In",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String action = "set " + studentTimeFieldName(timeIn);
        if (!validateManualAttendanceAction(action)) {
            return;
        }

        LocalTime now = currentMinute();
        if (!timeIn && now.isBefore(student.timeIn)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Time Out cannot be earlier than Time In.",
                    "Invalid Time",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (timeIn && statusForTimeIn(now) == Status.ABSENT) {
            markAbsentWithoutTime(student, "Autosave: Marked absent");
            return;
        }

        applyStudentTime(student, timeIn, now);
    }

    void commitStudentTimeField(StudentRecord student, boolean timeIn, StudentCard.TimeEntryField field) {
        if (selectedDate == null) {
            field.refresh();
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a date first before setting a time.",
                    "Select Date",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!timeIn && student.timeIn == null) {
            field.beginEdit();
            JOptionPane.showMessageDialog(
                    this,
                    "Set Time In first before setting Time Out.",
                    "Missing Time In",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalTime parsed = parseTypedTime(field.getText());
        if (parsed == null) {
            field.beginEdit();
            String fieldName = studentTimeFieldName(timeIn);
            JOptionPane.showMessageDialog(
                    this,
                    "Type a valid " + fieldName + ", like 8:05 AM or 13:30.",
                    "Invalid Time",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (applyStudentTime(student, timeIn, parsed)) {
            field.finishEdit();
        } else {
            field.beginEdit();
        }
    }

    private boolean applyStudentTime(StudentRecord student, boolean editingTimeIn, LocalTime value) {
        if (value == null) {
            return false;
        }

        LocalTime normalized = value.withSecond(0).withNano(0);
        if (!validateTimeSelection(normalized, editingTimeIn)) {
            return false;
        }
        if (!editingTimeIn && student.timeIn != null && normalized.isBefore(student.timeIn)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Time Out cannot be earlier than Time In.",
                    "Invalid Time",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        Status oldStatus = student.status;
        LocalTime oldTimeIn = student.timeIn;
        LocalTime oldTimeOut = student.timeOut;

        if (editingTimeIn) {
            Status newStatus = statusForTimeIn(normalized);
            student.status = newStatus;
            if (newStatus == Status.ABSENT) {
                student.timeIn = null;
                student.timeOut = null;
            } else {
                student.timeIn = normalized;
                if (student.timeOut != null && student.timeOut.isBefore(student.timeIn)) {
                    student.timeOut = null;
                }
            }
        } else {
            student.timeOut = normalized;
            if (student.status == Status.UNMARKED && student.timeIn != null) {
                student.status = statusForTimeIn(student.timeIn);
            }
        }

        if (student.status == Status.UNMARKED || student.status == Status.ABSENT || student.status == Status.EXCUSED) {
            if (student.seatNumber > 0) {
                String cType = getClassroomType();
                db.removeSeat(student.studentId, selectedClass.sectionSubjectId, cType);
                student.seatNumber = 0;
            }
            updateClassroomTypeLock();
        } else {
            if (student.seatNumber <= 0) {
                String cType = getClassroomType();
                student.seatNumber = db.getRandomAvailableSeat(selectedClass.sectionSubjectId, cType);
                if (student.seatNumber > 0) {
                    db.assignSeat(student.studentId, selectedClass.sectionSubjectId, cType, student.seatNumber);
                    lockClassroomType();
                }
            }
        }

        adjustAbsenceCount(student, oldStatus, student.status);

        StudentCard card = findStudentCard(student.studentId);
        refreshStudentCard(card);
        updateCounts(gridPanel.getComponentCount());
        setAutosave("Saving...", YELLOW);

        if (persistStatus(student)) {
            setAutosave("Autosave: All changes saved", GREEN);
            if (app.getSeatendance() != null) {
                app.getSeatendance().refreshClassroomBuilder();
                refreshClassroomFrame();
                String action = editingTimeIn ? "Time In" : "Time Out";
                String subjectInfo = selectedClass.subjectCode + " \u2013 " + selectedClass.sectionName;
                LocalTime displayTime = editingTimeIn ? student.timeIn : student.timeOut;
                app.getSeatendance().pushRecentActivity(
                        student.name,
                        subjectInfo,
                        student.status.label,
                        action,
                        formatTime(displayTime));
            }
        } else {
            adjustAbsenceCount(student, student.status, oldStatus);
            restoreStudentState(student, oldStatus, oldTimeIn, oldTimeOut);
            refreshStudentCard(card);
            setAutosave("Autosave failed", RED);
            return false;
        }

        refreshVisibleCards();
        return true;
    }

    private boolean markAbsentWithoutTime(StudentRecord student, String savedMessage) {
        Status oldStatus = student.status;
        LocalTime oldTimeIn = student.timeIn;
        LocalTime oldTimeOut = student.timeOut;

        student.status = Status.ABSENT;
        student.timeIn = null;
        student.timeOut = null;
        adjustAbsenceCount(student, oldStatus, student.status);

        StudentCard card = findStudentCard(student.studentId);
        refreshStudentCard(card);
        updateCounts(gridPanel.getComponentCount());
        setAutosave("Saving...", YELLOW);

        if (persistStatus(student)) {
            setAutosave(savedMessage, GREEN);
            if (app.getSeatendance() != null) {
                app.getSeatendance().refreshClassroomBuilder();
                refreshClassroomFrame();
                String subjectInfo = selectedClass.subjectCode + " \u2013 " + selectedClass.sectionName;
                app.getSeatendance().pushRecentActivity(
                        student.name,
                        subjectInfo,
                        Status.ABSENT.label,
                        "Time In",
                        formatTime(null));
            }
            refreshVisibleCards();
            return true;
        }

        adjustAbsenceCount(student, student.status, oldStatus);
        restoreStudentState(student, oldStatus, oldTimeIn, oldTimeOut);
        refreshStudentCard(card);
        setAutosave("Autosave failed", RED);
        refreshVisibleCards();
        return false;
    }

    private LocalTime currentMinute() {
        return LocalTime.now().withSecond(0).withNano(0);
    }

    private LocalTime selectedStartTime() {
        return timeFieldValue(startTimeField);
    }

    private LocalTime timeFieldValue(TimeTextField field) {
        if (field == null) {
            return null;
        }
        return field.timeValue();
    }

    private LocalTime selectedEndTime() {
        return timeFieldValue(endTimeField);
    }

    private int selectedLateMinutes() {
        if (lateIntervalField == null) {
            return 15;
        }
        return lateIntervalField.minuteValue();
    }

    private int selectedAbsentMinutes() {
        if (absentIntervalField == null) {
            return 45;
        }
        return absentIntervalField.minuteValue();
    }

    private boolean isClassWindowValid() {
        LocalTime start = selectedStartTime();
        LocalTime end = selectedEndTime();
        return start != null && end != null && end.isAfter(start);
    }

    private boolean isInsideClassWindow(LocalTime time) {
        if (time == null || !isClassWindowValid()) {
            return false;
        }
        LocalTime start = selectedStartTime();
        LocalTime end = selectedEndTime();
        return !time.isBefore(start) && !time.isAfter(end);
    }

    private boolean validateManualAttendanceAction(String action) {
        if (concluded) {
            return true;
        }
        return validateClassWindow(currentMinute(), action);
    }

    private boolean validateClassWindow(LocalTime time, String action) {
        if (!isClassWindowValid()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Choose a valid class time first. Class Ends must be later than Class Starts.",
                    "Invalid Class Time",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!isInsideClassWindow(time)) {
            JOptionPane.showMessageDialog(
                    this,
                    "You can only " + action + " from " + formatTime(selectedStartTime()) + " to "
                            + formatTime(selectedEndTime()) + ".",
                    "Outside Class Time",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean validateTimeSelection(LocalTime time, boolean timeIn) {
        if (!isClassWindowValid()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Choose a valid class time first. Class Ends must be later than Class Starts.",
                    "Invalid Class Time",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!isInsideClassWindow(time)) {
            String fieldName = studentTimeFieldName(timeIn);
            JOptionPane.showMessageDialog(
                    this,
                    "Choose a valid " + fieldName + " from "
                            + formatTime(selectedStartTime()) + " to " + formatTime(selectedEndTime()) + ".",
                    "Invalid Time",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private Status statusForTimeIn(LocalTime timeIn) {
        LocalTime start = selectedStartTime();
        if (start == null) {
            return Status.PRESENT;
        }
        if (timeIn.isAfter(start.plusMinutes(selectedAbsentMinutes()))) {
            return Status.ABSENT;
        }
        if (timeIn.isAfter(start.plusMinutes(selectedLateMinutes()))) {
            return Status.LATE;
        }
        return Status.PRESENT;
    }

    String formatTime(LocalTime time) {
        if (time == null) {
            return "--:--";
        }
        return time.format(TIME_FORMAT);
    }

    String cardTimeText(boolean timeIn, LocalTime time) {
        if (time == null) {
            return studentTimeFieldName(timeIn);
        }
        if (timeIn) {
            return "In " + formatTime(time);
        }
        return "Out " + formatTime(time);
    }

    String studentTimeFieldName(boolean timeIn) {
        if (timeIn) {
            return "Time In";
        }
        return "Time Out";
    }

    private void commitClassTimeField(TimeTextField field) {
        if (updatingTimeControl || field == null || field.isSoftDisabled()) {
            return;
        }

        LocalTime oldValue = field.timeValue();
        String input = field.getText().trim();
        if (input.isEmpty()) {
            field.clearTimeValue();
            refreshTimeBasedStatuses();
            return;
        }

        LocalTime parsed = parseTypedTime(input);
        if (parsed == null) {
            field.restoreTimeValue(oldValue);
            JOptionPane.showMessageDialog(
                    this,
                    "Type a valid class time, like 7:30 AM, 8am, or 13:30.",
                    "Invalid Time",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        field.setTimeValue(parsed);
        LocalTime start = selectedStartTime();
        LocalTime end = selectedEndTime();
        if (start != null && end != null && !end.isAfter(start)) {
            field.restoreTimeValue(oldValue);
            JOptionPane.showMessageDialog(
                    this,
                    "Class Ends must be later than Class Starts.",
                    "Invalid Class Time",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshTimeBasedStatuses();
        saveSessionTimesToDb();
    }

    private void saveSessionTimesToDb() {
        if (selectedClass == null || selectedDate == null)
            return;
        String startStr = null;
        String endStr = null;
        if (startTimeField != null && startTimeField.timeValue() != null) {
            startStr = startTimeField.getText().trim();
        }
        if (endTimeField != null && endTimeField.timeValue() != null) {
            endStr = endTimeField.getText().trim();
        }
        String cType = getClassroomType();
        System.out.println("[DEBUG] Saving times: ssid=" + selectedClass.sectionSubjectId
                + " date=" + selectedSqlDate() + " start=" + startStr + " end=" + endStr + " type=" + cType);
        db.updateSessionTimes(selectedClass.sectionSubjectId, selectedSqlDate(), startStr, endStr, cType);
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT).withSecond(0).withNano(0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private LocalTime parseTypedTime(String value) {
        String input = "";
        if (value != null) {
            input = value.trim();
        }
        if (input.isEmpty()) {
            return null;
        }

        String normalized = input.toUpperCase(Locale.ENGLISH).replaceAll("\\s+", " ");
        String compact = normalized.replace(" ", "");
        String[] candidates = { normalized, compact };
        String[] patterns = { "h:mm a", "h:mma", "h a", "ha", "H:mm", "H" };

        for (String candidate : candidates) {
            for (String pattern : patterns) {
                try {
                    return LocalTime.parse(candidate, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
                            .withSecond(0)
                            .withNano(0);
                } catch (RuntimeException e) {
                    // Try the next common time format.
                }
            }
        }
        return null;
    }

    private int parseMinutesInput(String value) {
        if (value == null || value.trim().isEmpty()) {
            return -1;
        }
        String cleaned = value.trim()
                .toLowerCase(Locale.ENGLISH)
                .replaceAll("\\s+", "")
                .replaceAll("minutes$", "")
                .replaceAll("mins$", "")
                .replaceAll("min$", "")
                .replaceAll("m$", "");
        try {
            int result = Integer.parseInt(cleaned);
            if (result >= 0) {
                return result;
            }
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void commitMinuteField(MinuteTextField field) {
        if (field == null || field.isSoftDisabled()) {
            return;
        }
        String input = field.getText().trim();
        if (input.isEmpty()) {
            field.setMinuteValue(field.minuteValue());
            return;
        }
        int parsed = parseMinutesInput(input);
        if (parsed < 0) {
            field.setMinuteValue(field.minuteValue());
            JOptionPane.showMessageDialog(
                    this,
                    "Type a valid number of minutes, like 5, 15min, or 30 minutes.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        field.setMinuteValue(parsed);
        refreshTimeBasedStatuses();
    }

    private void styleScrollBar(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }
        scrollBar.setOpaque(false);
        scrollBar.setPreferredSize(new Dimension(s(10), s(10)));
        scrollBar.setUnitIncrement(s(16));
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(70, 80, 90);
                trackColor = BG;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private JButton zeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
    }

    private Image loadImage(String path) {
        java.net.URL resource = getClass().getResource(path);
        if (resource == null) {
            return null;
        }
        return new ImageIcon(resource).getImage();
    }

    Font font(int style, int size) {
        return new Font("Segoe UI", style, Math.max(10, s(size)));
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    Color blend(Color from, Color to, double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        int red = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int green = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int blue = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        return new Color(red, green, blue);
    }

    void paintRoundedInput(Graphics g, Component component, boolean enabled, int radius) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (enabled) {
            g2.setColor(CONTROL_BG);
        } else {
            g2.setColor(DISABLED_CONTROL_BG);
        }
        g2.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), s(radius), s(radius));
        if (enabled) {
            g2.setColor(BORDER);
        } else {
            g2.setColor(DISABLED_BORDER);
        }
        g2.drawRoundRect(1, 1, component.getWidth() - 3, component.getHeight() - 3, s(radius), s(radius));
        g2.dispose();
    }

    // -------------------------------------------------------------------------
    // NEW: Classroom frame methods
    // -------------------------------------------------------------------------

    private static final class ClassroomFrame extends ScaledFrame {
        ClassroomFrame(double scale) {
            super(scale);
        }
    }

    private void showClassroomFrame() {
        if (selectedClass == null || selectedDate == null)
            return;

        java.util.Map<Integer, String> occupancy = new java.util.HashMap<>();
        for (StudentRecord student : students) {
            if (student.seatNumber > 0 && student.status != Status.UNMARKED) {
                occupancy.put(student.seatNumber, student.name);
            }
        }

        String roomType = getClassroomType();
        String teacher = teacherNameOrUnknown(selectedClass);

        if (classroomFrame == null) {
            classroomFrame = new ClassroomFrame(scale);
            classroomFrame.setTitle("Classroom View");
            classroomFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            classroomFrame.setSize(s(900), s(700));
            classroomFrame.setLocationRelativeTo(this);
            classroomFrame.getContentPane().setBackground(BG);
        }

        classroomFrame.getContentPane().removeAll();
        Classroom classroom = new Classroom(scale, roomType, occupancy, teacher);
        classroomFrame.getContentPane().add(classroom, BorderLayout.CENTER);
        classroomFrame.revalidate();
        classroomFrame.repaint();
        classroomFrame.setVisible(true);
    }

    private void hideClassroomFrame() {
        if (classroomFrame != null) {
            classroomFrame.setVisible(false);
        }
    }

    public void refreshClassroomFrame() {
        if (classroomFrame == null || !classroomFrame.isVisible())
            return;
        showClassroomFrame();
    }

    // -------------------------------------------------------------------------
    // Inner classes (unchanged)
    // -------------------------------------------------------------------------

    private final class CalendarPicker extends JDialog {
        private final LocalDate currentSelection;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel daysPanel = new JPanel(new GridLayout(0, 7, s(6), s(6)));
        private YearMonth visibleMonth;
        private LocalDate chosenDate;

        private CalendarPicker(Window owner, LocalDate currentSelection) {
            super(owner, "Select Date", ModalityType.APPLICATION_MODAL);
            this.currentSelection = currentSelection;
            LocalDate startingDate;
            if (currentSelection == null) {
                startingDate = LocalDate.now();
            } else {
                startingDate = currentSelection;
            }
            this.visibleMonth = YearMonth.from(startingDate);

            setUndecorated(false);
            setResizable(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            RoundedPanel root = new RoundedPanel(FOOTER_BG, CALENDAR_BORDER, PANEL_RADIUS, 1);
            root.setLayout(new BorderLayout(0, s(12)));
            root.setBorder(new EmptyBorder(s(14), s(16), s(14), s(16)));

            root.add(buildCalendarHeader(), BorderLayout.NORTH);
            root.add(daysPanel, BorderLayout.CENTER);
            root.add(buildCalendarLegend(), BorderLayout.SOUTH);
            setContentPane(root);
            refreshCalendar();
            pack();
            setLocationRelativeTo(StartSessionPanel.this);
        }

        private LocalDate showDialog() {
            setVisible(true);
            return chosenDate;
        }

        private JPanel buildCalendarHeader() {
            JPanel header = new JPanel(new BorderLayout(s(12), 0));
            header.setOpaque(false);

            JButton previous = calendarNavButton("<");
            previous.addActionListener(e -> {
                visibleMonth = visibleMonth.minusMonths(1);
                refreshCalendar();
            });

            JButton next = calendarNavButton(">");
            next.addActionListener(e -> {
                visibleMonth = visibleMonth.plusMonths(1);
                refreshCalendar();
            });

            monthLabel.setForeground(TEXT);
            monthLabel.setFont(font(Font.BOLD, 16));

            header.add(previous, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);
            return header;
        }

        private JPanel buildCalendarLegend() {
            JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, s(14), 0));
            legend.setOpaque(false);
            legend.add(calendarLegendItem("Selected", CAL_SELECTED_BORDER));
            legend.add(calendarLegendItem("Concluded", CAL_CONCLUDED_BORDER));
            legend.add(calendarLegendItem("In Progress", CAL_DRAFT_BORDER));
            return legend;
        }

        private JPanel calendarLegendItem(String text, Color color) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, s(6), 0));
            item.setOpaque(false);
            item.add(new StatusDot(color, 8));
            JLabel label = new JLabel(text);
            label.setForeground(MUTED);
            label.setFont(font(Font.PLAIN, 12));
            item.add(label);
            return item;
        }

        private JButton calendarNavButton(String text) {
            JButton button = new JButton(text);
            Dimension size = new Dimension(s(40), s(30));
            setFixedSize(button, size);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            button.setForeground(TEXT);
            button.setFont(font(Font.BOLD, 16));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return button;
        }

        private void refreshCalendar() {
            monthLabel.setText(visibleMonth.atDay(1)
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));
            daysPanel.removeAll();
            daysPanel.setOpaque(false);

            String[] weekdays = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
            for (String weekday : weekdays) {
                JLabel label = new JLabel(weekday, SwingConstants.CENTER);
                label.setForeground(META);
                label.setFont(font(Font.BOLD, 11));
                daysPanel.add(label);
            }

            ArrayList<SessionStateItem> states = loadSessionStateItems();

            LocalDate firstDay = visibleMonth.atDay(1);
            int blanks = firstDay.getDayOfWeek().getValue() % 7;
            for (int i = 0; i < blanks; i++) {
                JLabel blank = new JLabel();
                blank.setPreferredSize(new Dimension(s(38), s(34)));
                daysPanel.add(blank);
            }

            for (int day = 1; day <= visibleMonth.lengthOfMonth(); day++) {
                LocalDate date = visibleMonth.atDay(day);
                JButton button = calendarDayButton(date, findSessionState(states, date));
                daysPanel.add(button);
            }

            daysPanel.revalidate();
            daysPanel.repaint();
            pack();
        }

        private ArrayList<SessionStateItem> loadSessionStateItems() {
            ArrayList<SessionStateItem> states = new ArrayList<>();
            if (selectedClass == null) {
                return states;
            }

            LocalDate firstDateOfMonth = visibleMonth.atDay(1);
            for (java.util.Map.Entry<LocalDate, String> entry : db
                    .getAttendanceSessionStatesForMonth(selectedClass.sectionSubjectId, firstDateOfMonth)
                    .entrySet()) {
                SessionStateItem item = new SessionStateItem(entry.getKey(), entry.getValue());
                states.add(item);
            }
            return states;
        }

        private String findSessionState(ArrayList<SessionStateItem> states, LocalDate date) {
            for (SessionStateItem item : states) {
                if (item.date.equals(date)) {
                    return item.state;
                }
            }
            return null;
        }

        private JButton calendarDayButton(LocalDate date, String state) {
            boolean selected = date.equals(currentSelection);
            Color background = calendarDayBackground(selected, state);
            Color border = calendarDayBorder(selected, state);

            JButton button = new JButton(String.valueOf(date.getDayOfMonth())) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(background);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(CONTROL_RADIUS), s(CONTROL_RADIUS));
                    g2.setColor(border);
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, s(CONTROL_RADIUS), s(CONTROL_RADIUS));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            button.setPreferredSize(new Dimension(s(38), s(34)));
            button.setForeground(TEXT);
            button.setFont(font(Font.BOLD, 12));
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(e -> {
                chosenDate = date;
                dispose();
            });
            return button;
        }

        private Color calendarDayBackground(boolean selected, String state) {
            if (selected) {
                return CAL_SELECTED_BG;
            }
            if ("concluded".equalsIgnoreCase(state)) {
                return CAL_CONCLUDED_BG;
            }
            if ("draft".equalsIgnoreCase(state)) {
                return CAL_DRAFT_BG;
            }
            return CAL_EMPTY_BG;
        }

        private Color calendarDayBorder(boolean selected, String state) {
            if (selected) {
                return CAL_SELECTED_BORDER;
            }
            if ("concluded".equalsIgnoreCase(state)) {
                return CAL_CONCLUDED_BORDER;
            }
            if ("draft".equalsIgnoreCase(state)) {
                return CAL_DRAFT_BORDER;
            }
            return CAL_EMPTY_BORDER;
        }
    }

    JComponent profileIcon(int size, float alpha) {
        return new JComponent() {
            {
                Dimension dimension = new Dimension(s(size), s(size));
                setFixedSize(this, dimension);
                setAlignmentX(Component.CENTER_ALIGNMENT);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.0f, Math.min(1.0f, alpha))));
                if (profileImage != null) {
                    g2.drawImage(profileImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2.setColor(TEXT);
                    int head = getWidth() / 3;
                    g2.fillOval((getWidth() - head) / 2, s(2), head, head);
                    g2.fillRoundRect(s(5), getHeight() / 2, getWidth() - s(10), getHeight() / 3, s(10), s(10));
                }
                g2.dispose();
            }
        };
    }

    String fitText(String value, Font font, int maxWidth) {
        String text = "";
        if (value != null) {
            text = value;
        }
        FontMetrics fm = getFontMetrics(font);
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        int available = Math.max(0, maxWidth - ellipsisWidth);
        StringBuilder trimmed = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (fm.stringWidth(trimmed.toString() + ch) > available) {
                break;
            }
            trimmed.append(ch);
        }
        return trimmed.toString().trim() + ellipsis;
    }

    private final class HintTextField extends JTextField {
        private final String hint;

        private HintTextField(String hint) {
            this.hint = hint;
            setOpaque(false);
            setBorder(new EmptyBorder(0, s(14), 0, s(14)));
            setForeground(TEXT);
            setCaretColor(TEXT);
            setSelectionColor(TEXT_SELECTION_BG);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CONTROL_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(CONTROL_RADIUS), s(CONTROL_RADIUS));
            g2.setColor(BORDER);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, s(CONTROL_RADIUS), s(CONTROL_RADIUS));
            g2.dispose();
            super.paintComponent(g);

            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D hintGraphics = (Graphics2D) g.create();
                hintGraphics.setFont(getFont());
                hintGraphics.setColor(META);
                FontMetrics fm = hintGraphics.getFontMetrics();
                hintGraphics.drawString(hint, s(14), (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                hintGraphics.dispose();
            }
        }
    }

    private final class TimeTextField extends JTextField {
        private LocalTime lastValidTime;
        private boolean softDisabled;

        private TimeTextField(String selected) {
            lastValidTime = parseTime(selected);
            if (lastValidTime == null) {
                setText("");
            } else {
                setText(formatTime(lastValidTime));
            }
            setOpaque(false);
            setBorder(new EmptyBorder(0, s(12), 0, s(12)));
            setForeground(TEXT);
            setDisabledTextColor(META);
            setCaretColor(TEXT);
            setSelectionColor(TEXT_SELECTION_BG);
            setFont(font(Font.PLAIN, 14));
            setHorizontalAlignment(SwingConstants.CENTER);
            addActionListener(e -> commitClassTimeField(TimeTextField.this));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    commitClassTimeField(TimeTextField.this);
                }
            });
        }

        private boolean isSoftDisabled() {
            return softDisabled;
        }

        private void setSoftDisabled(boolean softDisabled) {
            this.softDisabled = softDisabled;
            setEditable(!softDisabled);
            setFocusable(!softDisabled);
            if (softDisabled) {
                setForeground(META);
                setCaretColor(META);
                setCursor(Cursor.getDefaultCursor());
            } else {
                setForeground(TEXT);
                setCaretColor(TEXT);
                setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            }
            repaint();
        }

        private LocalTime timeValue() {
            if (lastValidTime == null) {
                return null;
            }
            return lastValidTime.withSecond(0).withNano(0);
        }

        private void setTimeValue(LocalTime time) {
            updatingTimeControl = true;
            try {
                lastValidTime = time.withSecond(0).withNano(0);
                setText(formatTime(lastValidTime));
            } finally {
                updatingTimeControl = false;
            }
            repaint();
        }

        private void clearTimeValue() {
            updatingTimeControl = true;
            try {
                lastValidTime = null;
                setText("");
            } finally {
                updatingTimeControl = false;
            }
            repaint();
        }

        private void restoreTimeValue(LocalTime time) {
            if (time == null) {
                clearTimeValue();
            } else {
                setTimeValue(time);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintRoundedInput(g, this, !softDisabled, CONTROL_RADIUS);
            super.paintComponent(g);
        }
    }

    private final class MinuteTextField extends JTextField {
        private int lastValidMinutes;
        private boolean softDisabled;

        private MinuteTextField(int defaultMinutes) {
            lastValidMinutes = defaultMinutes;
            setText(defaultMinutes + " min");
            setOpaque(false);
            setBorder(new EmptyBorder(0, s(12), 0, s(12)));
            setForeground(TEXT);
            setDisabledTextColor(META);
            setCaretColor(TEXT);
            setSelectionColor(TEXT_SELECTION_BG);
            setFont(font(Font.PLAIN, 14));
            setHorizontalAlignment(SwingConstants.CENTER);
            addActionListener(e -> commitMinuteField(MinuteTextField.this));
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    commitMinuteField(MinuteTextField.this);
                }
            });
        }

        private boolean isSoftDisabled() {
            return softDisabled;
        }

        private void setSoftDisabled(boolean disabled) {
            this.softDisabled = disabled;
            setEditable(!disabled);
            setFocusable(!disabled);
            if (disabled) {
                setForeground(META);
                setCaretColor(META);
                setCursor(Cursor.getDefaultCursor());
            } else {
                setForeground(TEXT);
                setCaretColor(TEXT);
                setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            }
            repaint();
        }

        private int minuteValue() {
            return lastValidMinutes;
        }

        private void setMinuteValue(int minutes) {
            lastValidMinutes = minutes;
            setText(minutes + " min");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintRoundedInput(g, this, !softDisabled, CONTROL_RADIUS);
            super.paintComponent(g);
        }
    }

    private final class StyledComboBox<E> extends JComboBox<E> {
        private boolean softDisabled;

        private StyledComboBox(E[] items) {
            super(items);
            setOpaque(false);
            setBackground(CONTROL_BG);
            setForeground(TEXT);
            setBorder(new EmptyBorder(s(1), s(10), s(1), s(8)));
            setUI(new BasicComboBoxUI() {
                @Override
                public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                }

                @Override
                @SuppressWarnings({ "rawtypes", "unchecked" })
                protected ComboPopup createPopup() {
                    return new BasicComboPopup((JComboBox) comboBox) {
                        @Override
                        protected JScrollPane createScroller() {
                            JScrollPane scroller = super.createScroller();
                            scroller.setOpaque(true);
                            scroller.setBackground(CONTROL_BG);
                            scroller.setBorder(new LineBorder(BORDER));
                            scroller.getViewport().setOpaque(true);
                            scroller.getViewport().setBackground(CONTROL_BG);
                            styleScrollBar(scroller.getVerticalScrollBar());
                            styleScrollBar(scroller.getHorizontalScrollBar());
                            return scroller;
                        }
                    };
                }

                @Override
                protected JButton createArrowButton() {
                    JButton button = new JButton() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setStroke(new BasicStroke(Math.max(1, s(2)), BasicStroke.CAP_ROUND,
                                    BasicStroke.JOIN_ROUND));
                            if (visuallyEnabled()) {
                                g2.setColor(MUTED);
                            } else {
                                g2.setColor(META);
                            }
                            int centerX = getWidth() / 2;
                            int centerY = getHeight() / 2;
                            int half = s(4);
                            g2.drawLine(centerX - half, centerY - s(2), centerX, centerY + s(3));
                            g2.drawLine(centerX, centerY + s(3), centerX + half, centerY - s(2));
                            g2.dispose();
                        }
                    };
                    button.setPreferredSize(new Dimension(s(28), 0));
                    button.setBorderPainted(false);
                    button.setContentAreaFilled(false);
                    button.setFocusPainted(false);
                    button.setForeground(MUTED);
                    return button;
                }
            });
        }

        private boolean isSoftDisabled() {
            return softDisabled;
        }

        private boolean visuallyEnabled() {
            return isEnabled() && !softDisabled;
        }

        private void setSoftDisabled(boolean softDisabled) {
            this.softDisabled = softDisabled;
            if (softDisabled) {
                setForeground(META);
                setCursor(Cursor.getDefaultCursor());
            } else {
                setForeground(TEXT);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            Component editor = getEditor().getEditorComponent();
            if (editor instanceof JTextField) {
                JTextField field = (JTextField) editor;
                field.setEditable(!softDisabled);
                field.setFocusable(!softDisabled);
                if (softDisabled) {
                    field.setForeground(META);
                    field.setCaretColor(META);
                } else {
                    field.setForeground(TEXT);
                    field.setCaretColor(TEXT);
                }
                field.repaint();
            }
            repaint();
        }

        @Override
        public void showPopup() {
            if (softDisabled) {
                return;
            }
            super.showPopup();
        }

        @Override
        public void setPopupVisible(boolean visible) {
            if (visible && softDisabled) {
                return;
            }
            super.setPopupVisible(visible);
        }

        @Override
        public void processKeyEvent(KeyEvent e) {
            if (softDisabled) {
                e.consume();
                return;
            }
            super.processKeyEvent(e);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (visuallyEnabled()) {
                g2.setColor(CONTROL_BG);
            } else {
                g2.setColor(DISABLED_CONTROL_BG);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), s(CONTROL_RADIUS), s(CONTROL_RADIUS));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (visuallyEnabled()) {
                g2.setColor(BORDER);
            } else {
                g2.setColor(DISABLED_BORDER);
            }
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, s(CONTROL_RADIUS), s(CONTROL_RADIUS));
            g2.dispose();
        }
    }

    private final class ActionButton extends JButton {
        private final Color bg;
        private final Color border;
        private final Color fg;
        private final Timer hoverTimer;
        private boolean hovered;
        private double hoverProgress;

        private ActionButton(String text, Color bg, Color border, Color fg) {
            super(text);
            this.bg = bg;
            this.border = border;
            this.fg = fg;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(fg);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
            double target = 0.0;
            if (hovered && isEnabled()) {
                target = 1.0;
            }
            double step = 0.18;

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

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double progress = 0.0;
            if (isEnabled()) {
                progress = hoverProgress;
            }
            int inset = (int) Math.round(s(3) * (1.0 - progress));
            int x = inset;
            int y = inset;
            int width = getWidth() - inset * 2;
            int height = getHeight() - inset * 2;

            Color fill = blend(bg, brighten(bg), progress);
            if (!isEnabled()) {
                fill = new Color(44, 49, 54);
            }
            g2.setColor(fill);
            g2.fillRoundRect(x, y, width, height, s(CONTROL_RADIUS), s(CONTROL_RADIUS));
            if (isEnabled()) {
                g2.setColor(border);
            } else {
                g2.setColor(new Color(58, 65, 72));
            }
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, s(CONTROL_RADIUS), s(CONTROL_RADIUS));

            if (isEnabled()) {
                g2.setColor(fg);
            } else {
                g2.setColor(META);
            }
            Font paintFont = getFont().deriveFont((float) (getFont().getSize2D() + Math.max(1, s(1)) * progress));
            g2.setFont(paintFont);
            FontMetrics fm = g2.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(getText())) / 2;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), textX, textY);
            g2.dispose();
        }

        private Color brighten(Color color) {
            return new Color(
                    Math.min(255, color.getRed() + 22),
                    Math.min(255, color.getGreen() + 22),
                    Math.min(255, color.getBlue() + 22));
        }
    }

    private final class StatusDot extends JComponent {
        private final Color color;

        private StatusDot(Color color, int size) {
            this.color = color;
            Dimension dimension = new Dimension(s(size), s(size));
            setFixedSize(this, dimension);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
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
            int stroke = Math.max(1, s(borderWidth));
            g2.setStroke(new BasicStroke(stroke));
            int inset = Math.max(1, (stroke + 1) / 2);
            int width = Math.max(0, getWidth() - inset * 2 - 1);
            int height = Math.max(0, getHeight() - inset * 2 - 1);
            g2.drawRoundRect(inset, inset, width, height, s(radius), s(radius));
            g2.dispose();
        }
    }

    private final class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        private RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component component, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, s(radius), s(radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }

    private final class WrapLayout extends FlowLayout {
        private WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= getHgap() + 1;
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth <= 0 && target.getParent() != null) {
                    targetWidth = target.getParent().getWidth();
                }
                if (targetWidth <= 0) {
                    targetWidth = StartSessionPanel.this.s(1100);
                }

                Insets insets = target.getInsets();
                int horizontalInsets = insets.left + insets.right + getHgap() * 2;
                int maxWidth = Math.max(1, targetWidth - horizontalInsets);
                int rowWidth = 0;
                int rowHeight = 0;
                Dimension dimension = new Dimension(0, 0);

                for (Component component : target.getComponents()) {
                    if (!component.isVisible()) {
                        continue;
                    }

                    Dimension componentSize;
                    if (preferred) {
                        componentSize = component.getPreferredSize();
                    } else {
                        componentSize = component.getMinimumSize();
                    }
                    if (rowWidth + componentSize.width > maxWidth && rowWidth > 0) {
                        addRow(dimension, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) {
                        rowWidth += getHgap();
                    }
                    rowWidth += componentSize.width;
                    rowHeight = Math.max(rowHeight, componentSize.height);
                }

                addRow(dimension, rowWidth, rowHeight);
                dimension.width += horizontalInsets;
                dimension.height += insets.top + insets.bottom + getVgap() * 2;
                return dimension;
            }
        }

        private void addRow(Dimension dimension, int rowWidth, int rowHeight) {
            dimension.width = Math.max(dimension.width, rowWidth);
            if (dimension.height > 0) {
                dimension.height += getVgap();
            }
            dimension.height += rowHeight;
        }
    }

    private final class ScrollableGridPanel extends JPanel implements Scrollable {
        private ScrollableGridPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return s(28);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(s(120), visibleRect.height - s(40));
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}