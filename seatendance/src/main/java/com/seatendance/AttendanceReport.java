package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AttendanceReport extends ScaledPanel {

    private static final Color BG = new Color(34, 38, 42);
    private static final Color CARD_BG = new Color(20, 25, 28);
    private static final Color TABLE_ROW_BG = new Color(19, 24, 27);
    private static final Color TABLE_ROW_ALT_BG = new Color(21, 26, 30);
    private static final Color CONTROL_BG = new Color(25, 31, 36);
    private static final Color CONTROL_HOVER_BG = new Color(33, 40, 47);
    private static final Color CONTROL_BORDER = new Color(78, 88, 98);
    private static final Color BORDER = new Color(63, 70, 78);
    private static final Color TABLE_LINE = new Color(43, 52, 60);
    private static final Color TABLE_ROW_LINE = new Color(35, 42, 49);
    private static final Color TABLE_VERTICAL_LINE = new Color(27, 33, 38);
    private static final Color TABLE_HEADER_BG = new Color(20, 25, 28);
    private static final Color TEXT = Color.WHITE;
    private static final Color MUTED = new Color(174, 182, 193);
    private static final Color META_TEXT = new Color(137, 148, 160);
    private static final Color GREEN = new Color(39, 201, 120);
    private static final Color ORANGE = new Color(255, 178, 56);
    private static final Color RED_ORANGE = new Color(255, 98, 42);
    private static final Color BLUE = new Color(75, 154, 255);
    private static final Color PINK = new Color(244, 83, 151);
    private static final Color BRAND_RED = new Color(190, 40, 49);

    private final DatabaseHandler db;
    private final java.util.List<ClassOption> classOptions;

    private DefaultTableModel tableModel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;
    private JComboBox<ClassOption> classCombo;
    private HintTextField searchField;
    private JComboBox<String> statusFilter;
    private JLabel instructorValueLabel;
    private JLabel generatedLabel;
    private JLabel studentCountLabel;
    private JLabel recipientLabel;
    private JButton emailBtn;
    private RoundedPanel tableCard;
    private JScrollPane tableScroll;

    private JLabel totalPresentLabel;
    private JLabel totalLateLabel;
    private JLabel totalAbsentLabel;
    private JLabel totalExcusedLabel;
    private JLabel warningLabel;
    private JLabel fdaLabel;

    private int sectionSubjectId;
    private String displayLabel;
    private String teacherEmail;
    private ClassOption selectedClass;

    private String loggedInEmail;

    public AttendanceReport(DatabaseHandler db, double scale, java.util.List<ClassOption> classOptions, String loggedInEmail) {
        super(scale);
        this.db = db;
        this.classOptions = new java.util.ArrayList<>(classOptions);
        this.loggedInEmail = loggedInEmail;

        setOpaque(true);
        setBackground(BG);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(0, s(10)));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(s(24), s(32), s(20), s(26)));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildReportBody(), BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);

        if (!this.classOptions.isEmpty()) {
            selectClass(this.classOptions.get(0));
        }
    }

    public static final class ClassOption {
        public final int sectionSubjectId;
        public final String sectionName;
        public final String subjectCode;
        public final String subjectName;
        public final String teacherName;

        public ClassOption(int sectionSubjectId, String sectionName, String subjectCode, String subjectName,
                String teacherName) {
            this.sectionSubjectId = sectionSubjectId;
            this.sectionName = safe(sectionName);
            this.subjectCode = safe(subjectCode);
            this.subjectName = safe(subjectName);
            this.teacherName = safe(teacherName);
        }

        private static String safe(String value) {
            return value == null ? "" : value.trim();
        }

        public String comboText() {
            return sectionName + " | " + subjectCode;
        }

        public String summaryText() {
            return sectionName + "  -  " + subjectCode + " " + subjectName;
        }

        public String emailLabel() {
            return sectionName + " - " + subjectCode + " " + subjectName + " (" + teacherName + ")";
        }

        @Override
        public String toString() {
            return comboText();
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel titleLine = new JPanel(new BorderLayout(s(14), 0));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Attendance Summary Report");
        title.setForeground(TEXT);
        title.setFont(font(Font.BOLD, 34));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLine.add(title, BorderLayout.WEST);

        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, s(14), 0));
        controlRow.setOpaque(false);
        controlRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        Font controlFont = font(Font.PLAIN, 14);
        Dimension instructorSize = new Dimension(controlWidthForInstructor(controlFont), s(40));
        Dimension classSize = new Dimension(controlWidthForClass(controlFont), s(40));

        JPanel instructorGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, s(8), 0));
        instructorGroup.setOpaque(false);
        JLabel instructorLabel = new JLabel("Instructor");
        instructorLabel.setForeground(MUTED);
        instructorLabel.setFont(font(Font.PLAIN, 13));
        instructorGroup.add(instructorLabel);

        RoundedPanel instructorBox = new RoundedPanel(CONTROL_BG, CONTROL_BORDER, 8, 1);
        instructorBox.setLayout(new GridBagLayout());
        instructorBox.setPreferredSize(instructorSize);
        instructorBox.setMinimumSize(instructorSize);
        instructorBox.setMaximumSize(instructorSize);
        instructorBox.setBorder(new EmptyBorder(0, s(12), 0, s(12)));
        instructorValueLabel = new JLabel("Select a class");
        instructorValueLabel.setForeground(TEXT);
        instructorValueLabel.setFont(controlFont);
        instructorBox.add(instructorValueLabel);
        instructorGroup.add(instructorBox);
        controlRow.add(instructorGroup);

        JPanel classGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, s(8), 0));
        classGroup.setOpaque(false);
        JLabel classLabel = new JLabel("Class");
        classLabel.setForeground(MUTED);
        classLabel.setFont(font(Font.PLAIN, 13));
        classGroup.add(classLabel);

        classCombo = new StyledComboBox<>(classOptions.toArray(new ClassOption[0]));
        classCombo.setPreferredSize(classSize);
        classCombo.setMinimumSize(classSize);
        classCombo.setMaximumSize(classSize);
        classCombo.setMaximumRowCount(8);
        classCombo.setForeground(TEXT);
        classCombo.setBackground(CONTROL_BG);
        classCombo.setFont(controlFont);
        classCombo.setFocusable(false);
        classCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ClassOption option) {
                    setText(option.comboText());
                    setToolTipText(option.summaryText());
                }
                setBorder(new EmptyBorder(s(7), s(10), s(7), s(10)));
                setBackground(isSelected ? CONTROL_HOVER_BG : CONTROL_BG);
                setForeground(TEXT);
                setOpaque(index >= 0);
                return this;
            }
        });
        classCombo.addActionListener(e -> {
            Object selected = classCombo.getSelectedItem();
            if (selected instanceof ClassOption option) {
                selectClass(option);
            }
        });
        classGroup.add(classCombo);
        controlRow.add(classGroup);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, s(10), 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dateLabel = new JLabel("Session Date: " + LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy (EEEE)", Locale.ENGLISH)));
        dateLabel.setForeground(META_TEXT);
        dateLabel.setFont(font(Font.PLAIN, 14));
        generatedLabel = new JLabel();
        generatedLabel.setForeground(META_TEXT);
        generatedLabel.setFont(font(Font.PLAIN, 14));
        metaRow.add(dateLabel);
        metaRow.add(new VerticalRule());
        metaRow.add(generatedLabel);

        left.add(titleLine);
        left.add(Box.createVerticalStrut(s(12)));
        left.add(controlRow);
        left.add(Box.createVerticalStrut(s(13)));
        left.add(metaRow);

        header.add(left, BorderLayout.CENTER);
        return header;
    }

    private int controlWidthForInstructor(Font controlFont) {
        int max = textWidth("Select a class", controlFont);
        for (ClassOption option : classOptions) {
            max = Math.max(max, textWidth(option.teacherName, controlFont));
        }
        return Math.max(s(190), Math.min(s(340), max + s(34)));
    }

    private int controlWidthForClass(Font controlFont) {
        int max = textWidth("Select a class", controlFont);
        for (ClassOption option : classOptions) {
            max = Math.max(max, textWidth(option.comboText(), controlFont));
        }
        return Math.max(s(300), Math.min(s(520), max + s(82)));
    }

    private int textWidth(String text, Font font) {
        FontMetrics fm = getFontMetrics(font);
        return fm.stringWidth(text == null ? "" : text);
    }

    private JPanel buildReportBody() {
        JPanel body = new JPanel(new BorderLayout(0, s(10)));
        body.setOpaque(false);
        body.add(buildStatsRow(), BorderLayout.NORTH);

        JPanel lower = new JPanel(new BorderLayout(0, s(8)));
        lower.setOpaque(false);
        lower.add(buildFilters(), BorderLayout.NORTH);
        lower.add(buildTablePanel(), BorderLayout.CENTER);
        body.add(lower, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, s(10), 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, s(104)));

        totalPresentLabel = metricLabel(GREEN);
        totalLateLabel = metricLabel(ORANGE);
        totalAbsentLabel = metricLabel(RED_ORANGE);
        totalExcusedLabel = metricLabel(BLUE);
        warningLabel = metricLabel(ORANGE);
        fdaLabel = metricLabel(PINK);

        row.add(statCard(totalPresentLabel, "Present", ReportIcon.PRESENT));
        row.add(statCard(totalLateLabel, "Late", ReportIcon.LATE));
        row.add(statCard(totalAbsentLabel, "Absent", ReportIcon.ABSENT));
        row.add(statCard(totalExcusedLabel, "Excused", ReportIcon.EXCUSED));
        row.add(statCard(warningLabel, "Warning", ReportIcon.WARNING));
        row.add(statCard(fdaLabel, "FDA Students", ReportIcon.FDA));
        return row;
    }

    private JLabel metricLabel(Color color) {
        JLabel label = new JLabel("0");
        label.setForeground(color);
        label.setFont(font(Font.BOLD, 30));
        return label;
    }

    private JPanel statCard(JLabel valueLabel, String labelText, ReportIcon icon) {
        RoundedPanel card = new RoundedPanel(CARD_BG, BORDER, 8, 1);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(s(12), s(16), s(12), s(16)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.insets = new Insets(0, 0, 0, s(14));
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(new IconImage(icon, 54), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, s(6), 0);
        gbc.anchor = GridBagConstraints.WEST;
        card.add(valueLabel, gbc);

        JLabel text = new JLabel(labelText);
        text.setForeground(TEXT);
        text.setFont(font(Font.PLAIN, 15));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(text, gbc);
        return card;
    }

    private JPanel buildFilters() {
        RoundedPanel filters = new RoundedPanel(CARD_BG, BORDER, 8, 1);
        filters.setLayout(new BorderLayout());
        filters.setBorder(new EmptyBorder(s(10), s(16), s(10), s(16)));
        filters.setPreferredSize(new Dimension(0, s(60)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, s(14), 0));
        left.setOpaque(false);

        searchField = new HintTextField("Search student...");
        searchField.setPreferredSize(new Dimension(s(280), s(38)));
        searchField.setForeground(TEXT);
        searchField.setCaretColor(TEXT);
        searchField.setBackground(CONTROL_BG);
        searchField.setFont(font(Font.PLAIN, 14));
        searchField.setBorder(new EmptyBorder(0, s(14), 0, s(14)));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });
        left.add(searchField);

        statusFilter = new StyledComboBox<>(new String[] { "All Statuses", "Good Standing", "Warning", "FDA" });
        statusFilter.setPreferredSize(new Dimension(s(204), s(38)));
        statusFilter.setMaximumSize(new Dimension(s(204), s(38)));
        statusFilter.setForeground(TEXT);
        statusFilter.setBackground(CONTROL_BG);
        statusFilter.setFont(font(Font.PLAIN, 14));
        statusFilter.setFocusable(false);
        statusFilter.addActionListener(e -> applyFilters());
        left.add(statusFilter);

        studentCountLabel = new JLabel("0 students");
        studentCountLabel.setForeground(MUTED);
        studentCountLabel.setFont(font(Font.BOLD, 14));

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(studentCountLabel);

        filters.add(left, BorderLayout.WEST);
        filters.add(right, BorderLayout.EAST);
        return filters;
    }

    private JPanel buildTablePanel() {
        tableCard = new RoundedPanel(CARD_BG, BORDER, 8, 1);
        tableCard.setLayout(new BorderLayout(0, s(14)));
        tableCard.setBorder(new EmptyBorder(s(14), s(18), s(14), s(18)));

        JLabel title = new JLabel("Attendance Records");
        title.setForeground(TEXT);
        title.setFont(font(Font.BOLD, 16));
        tableCard.add(title, BorderLayout.NORTH);
        tableCard.add(buildTable(), BorderLayout.CENTER);
        tableCard.add(buildFooter(), BorderLayout.SOUTH);
        return tableCard;
    }

    private JScrollPane buildTable() {
        String[] cols = { "#", "ID Number", "Student Name", "Present", "Late", "Absent", "Excused", "Status" };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || (columnIndex >= 3 && columnIndex <= 6)) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        table = new JTable(tableModel);
        table.setOpaque(true);
        table.setBackground(TABLE_ROW_BG);
        table.setForeground(TEXT);
        table.setFont(font(Font.PLAIN, 14));
        table.setRowHeight(s(66));
        table.setShowGrid(false);
        table.setGridColor(TABLE_ROW_LINE);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setFocusable(false);
        table.setSelectionBackground(TABLE_ROW_BG);
        table.setSelectionForeground(TEXT);
        table.setDefaultRenderer(Object.class, new ReportTableRenderer());
        table.setDefaultRenderer(Integer.class, new ReportTableRenderer());
        table.setFillsViewportHeight(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        int[] widths = { s(54), s(130), s(260), s(90), s(82), s(90), s(104), s(176) };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TEXT);
        header.setFont(font(Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, s(46)));
        header.setBorder(new MatteBorder(0, 0, 1, 0, TABLE_LINE));
        header.setDefaultRenderer(new HeaderRenderer());

        sorter = new TableRowSorter<>(tableModel);
        sorter.setComparator(2, this::compareStudentNames);
        sorter.setSortsOnUpdates(true);
        sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(2, SortOrder.ASCENDING)));
        table.setRowSorter(sorter);

        tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new RoundedLineBorder(TABLE_LINE, 6));
        tableScroll.setOpaque(false);
        tableScroll.setViewportBorder(null);
        tableScroll.getViewport().setBackground(TABLE_ROW_BG);
        tableScroll.setBackground(TABLE_ROW_BG);
        styleScrollBar(tableScroll.getVerticalScrollBar());
        styleScrollBar(tableScroll.getHorizontalScrollBar());
        return tableScroll;
    }

    private void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setOpaque(false);
        scrollBar.setPreferredSize(new Dimension(s(10), s(10)));
        scrollBar.setUnitIncrement(s(16));
        scrollBar.setUI(new DarkScrollBarUI());
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(s(14), 0));
        footer.setOpaque(false);
        footer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, TABLE_LINE),
                new EmptyBorder(s(10), 0, 0, 0)));

        RoundedPanel recipientPanel = new RoundedPanel(new Color(21, 27, 32), BORDER, 8, 1);
        recipientPanel.setLayout(new GridBagLayout());
        recipientPanel.setPreferredSize(new Dimension(s(360), s(38)));
        recipientPanel.setBorder(new EmptyBorder(0, s(14), 0, s(14)));

        recipientLabel = new JLabel("Recipient: (unknown)");
        recipientLabel.setForeground(MUTED);
        recipientLabel.setFont(font(Font.PLAIN, 13));
        recipientPanel.add(recipientLabel);
        footer.add(recipientPanel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, s(14), 0));
        actions.setOpaque(false);

        emailBtn = actionButton("Send Email Report", BRAND_RED, new Color(230, 72, 83));
        emailBtn.addActionListener(e -> sendEmailReport());

        actions.add(emailBtn);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private JButton actionButton(String text, Color bg, Color border) {
        JButton button = new HoverActionButton(text, bg, border);
        Dimension buttonSpace = new Dimension(s(224), s(50));
        button.setPreferredSize(buttonSpace);
        button.setMinimumSize(buttonSpace);
        button.setMaximumSize(buttonSpace);
        return button;
    }

    private void selectClass(ClassOption option) {
        if (option == null) {
            return;
        }

        selectedClass = option;
        sectionSubjectId = option.sectionSubjectId;
        displayLabel = option.emailLabel();
        teacherEmail = this.loggedInEmail;

        if (classCombo != null && classCombo.getSelectedItem() != option) {
            classCombo.setSelectedItem(option);
        }
        if (instructorValueLabel != null) {
            instructorValueLabel.setText(option.teacherName.isEmpty() ? "(unknown)" : option.teacherName);
            instructorValueLabel.setToolTipText(option.teacherName);
        }
        generatedLabel.setText("Generated on " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH)));
        recipientLabel.setText("Recipient: " + (hasTeacherEmail() ? teacherEmail : "(unknown)"));
        boolean canSendEmail = hasTeacherEmail();
        emailBtn.setEnabled(canSendEmail);
        emailBtn.setToolTipText(canSendEmail ? null : "No teacher email found for this class.");

        loadData(db.getAttendanceRows(sectionSubjectId));
    }

    public void refreshCurrentClass() {
        if (selectedClass == null) {
            return;
        }

        generatedLabel.setText("Generated on " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH)));
        loadData(db.getAttendanceRows(selectedClass.sectionSubjectId));
    }

    public void loadData(Object[][] rows) {
        tableModel.setRowCount(0);

        int totalPresent = 0;
        int totalLate = 0;
        int totalAbsent = 0;
        int totalExcused = 0;
        int warningCount = 0;
        int fdaCount = 0;

        if (rows != null) {
            for (Object[] row : rows) {
                tableModel.addRow(row);
                totalPresent += toInt(row[3]);
                totalLate += toInt(row[4]);
                totalAbsent += toInt(row[5]);
                totalExcused += toInt(row[6]);
                if (isWarningStatus(row[7])) {
                    warningCount++;
                } else if (isFdaStatus(row[7])) {
                    fdaCount++;
                }
            }
        }

        totalPresentLabel.setText(String.valueOf(totalPresent));
        totalLateLabel.setText(String.valueOf(totalLate));
        totalAbsentLabel.setText(String.valueOf(totalAbsent));
        totalExcusedLabel.setText(String.valueOf(totalExcused));
        warningLabel.setText(String.valueOf(warningCount));
        fdaLabel.setText(String.valueOf(fdaCount));
        applyFilters();
        sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(2, SortOrder.ASCENDING)));
        sorter.sort();
        SwingUtilities.invokeLater(() -> {
            if (table != null && table.getRowCount() > 0) {
                table.scrollRectToVisible(table.getCellRect(0, 0, true));
            }
            if (tableScroll != null) {
                tableScroll.getVerticalScrollBar().setValue(0);
            }
        });
    }

    private void applyFilters() {
        if (sorter == null) {
            return;
        }

        java.util.List<RowFilter<DefaultTableModel, Integer>> filters = new java.util.ArrayList<>();
        String query = searchField == null ? "" : searchField.getText().trim();
        if (!query.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 1, 2));
        }

        String selectedStatus = statusFilter == null ? "All Statuses" : String.valueOf(statusFilter.getSelectedItem());
        if ("FDA".equals(selectedStatus)) {
            filters.add(new StatusRowFilter("FDA"));
        } else if ("Warning".equals(selectedStatus)) {
            filters.add(new StatusRowFilter("Warning"));
        } else if ("Good Standing".equals(selectedStatus)) {
            filters.add(new StatusRowFilter("Good Standing"));
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        updateStudentCount();
    }

    private void updateStudentCount() {
        if (studentCountLabel == null || table == null) {
            return;
        }

        int count = table.getRowCount();
        studentCountLabel.setText(count + (count == 1 ? " student" : " students"));
    }

    private boolean hasTeacherEmail() {
        return teacherEmail != null && !teacherEmail.trim().isEmpty();
    }

    private void sendEmailReport() {
        if (!hasTeacherEmail()) {
            return;
        }

        String recipient = teacherEmail.trim();
        String body = buildEmailHtml();
        emailBtn.setEnabled(false);
        emailBtn.setText("Sending...");

        new Thread(() -> {
            boolean success = EmailSender.sendAttendanceReport(recipient, displayLabel, body);
            SwingUtilities.invokeLater(() -> {
                emailBtn.setEnabled(true);
                emailBtn.setText("Send Email Report");
                if (success) {
                    JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(AttendanceReport.this),
                            "Report sent to " + recipient,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(AttendanceReport.this),
                            "Failed to send email. Check your credentials.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private String buildEmailHtml() {
        String generatedOn = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH));
        String reportSubject = getEmailReportSubject();
        String instructorName = getEmailInstructorName();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=760, initial-scale=1.0\">");
        html.append("<style>");
        html.append("body{margin:0;padding:0;background:#eef0f4;color:#1f1f1f;width:100%;");
        html.append("font-family:'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;}");
        html.append("*{box-sizing:border-box;}");
        html.append(".email-wrap{width:100%;background:#eef0f4;padding:32px 0 40px;text-align:center;}");
        html.append(".email-card{width:760px;max-width:760px;margin:0 auto;background:#ffffff;");
        html.append("display:inline-block;text-align:left;vertical-align:top;");
        html.append("border-radius:8px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,.10);}");
        html.append(".brand{background:linear-gradient(135deg,#86141e 0%,#5a0d14 100%);");
        html.append("color:#ffffff;text-align:center;padding:28px 24px 24px;}");
        html.append(".brand-title{display:block;font-family:Georgia,'Times New Roman',serif;");
        html.append("font-size:34px;line-height:1.1;font-weight:bold;letter-spacing:0.5px;}");
        html.append(".brand-subtitle{display:block;margin-top:8px;font-size:13px;line-height:1.2;");
        html.append("font-weight:400;opacity:0.85;letter-spacing:0.3px;}");
        html.append(".content{padding:32px 36px 36px;}");
        html.append("h1{margin:0 0 6px;color:#1f1f1f;font-size:24px;line-height:1.2;");
        html.append("text-align:center;font-weight:700;letter-spacing:0.3px;}");
        html.append(".meta-box{margin:20px auto 0;max-width:420px;background:#f8f6f7;");
        html.append("border-left:4px solid #86141e;border-radius:0 6px 6px 0;padding:16px 20px;}");
        html.append(".meta-row{margin:0;color:#333333;font-size:14px;line-height:1.6;}");
        html.append(".meta-label{color:#777777;font-weight:400;}");
        html.append(".meta-val{font-weight:600;color:#1f1f1f;}");
        html.append(".generated{margin:16px 0 0;color:#999999;font-size:12px;text-align:center;}");
        html.append(".divider{height:2px;background:linear-gradient(90deg,transparent,#d8d8d8,transparent);");
        html.append("margin:28px 0 24px;}");
        html.append(".section-title{margin:0 0 16px;color:#86141e;font-size:13px;font-weight:700;");
        html.append("text-transform:uppercase;letter-spacing:1px;}");
        html.append(".summary-grid{width:100%;border-collapse:separate;");
        html.append("border-spacing:10px 0;margin:0 0 28px;table-layout:fixed;}");
        html.append(".summary-cell{width:16.66%;vertical-align:top;}");
        html.append(".metric{border-radius:8px;padding:18px 8px;text-align:center;border:1px solid #e8e8e8;}");
        html.append(".metric-present{background:#f0faf3;border-top:4px solid #168b35;}");
        html.append(".metric-late{background:#fef8ee;border-top:4px solid #d07a00;}");
        html.append(".metric-absent{background:#fef2f2;border-top:4px solid #9f1824;}");
        html.append(".metric-excused{background:#f0f4f8;border-top:4px solid #5b7a91;}");
        html.append(".metric-warning{background:#fff8e1;border-top:4px solid #d07a00;}");
        html.append(".metric-fda{background:#fdf0f1;border-top:4px solid #c41e2a;}");
        html.append(".metric-value{display:block;font-size:32px;line-height:1;font-weight:700;margin-bottom:6px;}");
        html.append(".metric-present .metric-value{color:#168b35;}");
        html.append(".metric-late .metric-value{color:#d07a00;}");
        html.append(".metric-absent .metric-value{color:#9f1824;}");
        html.append(".metric-excused .metric-value{color:#5b7a91;}");
        html.append(".metric-warning .metric-value{color:#d07a00;}");
        html.append(".metric-fda .metric-value{color:#c41e2a;}");
        html.append(".metric-label{display:block;font-size:10px;color:#666666;font-weight:600;");
        html.append("text-transform:uppercase;letter-spacing:0.2px;white-space:nowrap;word-break:keep-all;}");
        html.append(".attendance-table{width:100%;border-collapse:collapse;");
        html.append("border-radius:8px;overflow:hidden;font-size:13px;border:1px solid #e2e2e2;}");
        html.append(".attendance-table th{background:#f7f7f9;color:#444444;font-weight:600;");
        html.append("text-align:left;font-size:10px;text-transform:uppercase;letter-spacing:0.2px;");
        html.append("white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;}");
        html.append(".attendance-table td{padding:14px 14px;vertical-align:middle;");
        html.append("border-bottom:1px solid #f0f0f0;color:#333333;}");
        html.append(".attendance-table tbody tr:nth-child(even) td{background:#fafafa;}");
        html.append(".col-name{width:25%;}.col-id{width:15%;}");
        html.append(".col-count{width:9%;text-align:center;}.col-status{width:24%;text-align:center;}");
        html.append(".attendance-table th.col-count{text-align:center;}");
        html.append(".attendance-table th.col-status{text-align:center;}");
        html.append(".status-badge{display:inline-block;min-width:60px;padding:6px 10px;border-radius:20px;");
        html.append("font-weight:600;font-size:11px;text-align:center;text-transform:uppercase;letter-spacing:0.3px;}");
        html.append(".status-good{background:#e8f5e9;color:#2e7d32;}");
        html.append(".status-warning{background:#fff8e1;color:#e65100;}");
        html.append(".status-fda{background:#fce4ec;color:#c62828;}");
        html.append(".status-late{background:#fff8e1;color:#e65100;}");
        html.append(".status-absent{background:#ffebee;color:#b71c1c;}");
        html.append(".status-excused{background:#e8eef3;color:#37474f;}");
        html.append(".mobile-label{display:none;font-weight:600;color:#888888;font-size:11px;");
        html.append("text-transform:uppercase;letter-spacing:0.3px;margin-right:6px;}");
        html.append(".footer{background:#f7f7f9;border-top:3px solid #86141e;");
        html.append("margin-top:32px;padding:24px 36px;text-align:center;}");
        html.append(".footer-brand{color:#86141e;font-size:16px;font-weight:700;margin-bottom:8px;}");
        html.append(".footer-text{color:#888888;font-size:12px;line-height:1.7;}");
        html.append("</style></head><body>");

        // Use TABLE layout (not div) - Gmail respects width on tables, ignores it on divs
        html.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"width:100%;background:#eef0f4;padding:0;\">");
        html.append("<tr><td align=\"center\" style=\"padding:32px 0 40px;\">");
        html.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"760\" style=\"width:760px;min-width:760px;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,.10);\">");
        // Brand header
        html.append("<tr><td style=\"background:linear-gradient(135deg,#86141e 0%,#5a0d14 100%);color:#fff;text-align:center;padding:28px 24px 24px;\">");
        html.append("<span style=\"display:block;font-family:Georgia,serif;font-size:34px;line-height:1.1;font-weight:bold;\">SEAttendance</span>");
        html.append("<span style=\"display:block;margin-top:8px;font-size:13px;opacity:0.85;\">Seating Arrangement and Attendance Management System</span>");
        html.append("</td></tr>");
        // Content area
        html.append("<tr><td style=\"padding:32px 36px 36px;\">");
        html.append("<h1 style=\"margin:0 0 6px;color:#1f1f1f;font-size:24px;text-align:center;font-weight:700;\">Attendance Summary Report</h1>");
        html.append("<div class=\"meta-box\" style=\"margin:20px auto 0;max-width:420px;background:#f8f6f7;border-left:4px solid #86141e;border-radius:0 6px 6px 0;padding:16px 20px;\">");
        html.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;\">");
        html.append("<tr><td style=\"vertical-align:top;white-space:nowrap;color:#777;font-size:14px;line-height:1.6;padding-right:4px;\">Subject:</td>");
        html.append("<td style=\"vertical-align:top;font-weight:600;color:#1f1f1f;font-size:14px;line-height:1.6;\">").append(escapeHtml(reportSubject)).append("</td></tr>");
        if (!instructorName.isEmpty()) {
            html.append("<tr><td style=\"vertical-align:top;white-space:nowrap;color:#777;font-size:14px;line-height:1.6;padding-right:4px;\">Instructor:</td>");
            html.append("<td style=\"vertical-align:top;font-weight:600;color:#1f1f1f;font-size:14px;line-height:1.6;\">").append(escapeHtml(instructorName)).append("</td></tr>");
        }
        html.append("</table>");
        html.append("</div>");
        html.append("<div class=\"generated\" style=\"margin:16px 0 0;color:#999;font-size:12px;text-align:center;\">Generated on ").append(escapeHtml(generatedOn)).append("</div>");
        html.append("<div class=\"divider\" style=\"height:2px;background:linear-gradient(90deg,transparent,#d8d8d8,transparent);margin:28px 0 24px;\"></div>");
        html.append("<div class=\"section-title\" style=\"margin:0 0 16px;color:#86141e;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:1px;\">Overview</div>");
        html.append("<table class=\"summary-grid\" role=\"presentation\" cellspacing=\"10\" cellpadding=\"0\" width=\"688\" style=\"width:688px;min-width:688px;border-collapse:separate;border-spacing:10px 0;margin:0 auto 28px;\"><tr>");
        appendSummaryCard(html, "Present", totalPresentLabel.getText(), "metric-present");
        appendSummaryCard(html, "Late", totalLateLabel.getText(), "metric-late");
        appendSummaryCard(html, "Absent", totalAbsentLabel.getText(), "metric-absent");
        appendSummaryCard(html, "Excused", totalExcusedLabel.getText(), "metric-excused");
        appendSummaryCard(html, "Warning", warningLabel.getText(), "metric-warning");
        appendSummaryCard(html, "FDA", fdaLabel.getText(), "metric-fda");
        html.append("</tr></table>");
        html.append("<div class=\"section-title\" style=\"margin:0 0 16px;color:#86141e;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:1px;\">Student Records</div>");
        html.append("<table class=\"attendance-table\" cellpadding=\"0\" cellspacing=\"0\" width=\"688\" style=\"width:688px;min-width:688px;border-collapse:collapse;border-radius:8px;overflow:hidden;font-size:13px;border:1px solid #e2e2e2;margin:0 auto;\">");
        html.append("<thead><tr>");
        html.append("<th class=\"col-name\" style=\"background:#f7f7f9;color:#444;font-weight:600;text-align:left;font-size:10px;text-transform:uppercase;white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;min-width:150px;\">Student Name</th>");
        html.append("<th class=\"col-id\" style=\"background:#f7f7f9;color:#444;font-weight:600;text-align:left;font-size:10px;text-transform:uppercase;white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;min-width:100px;\">ID Number</th>");
        html.append("<th class=\"col-count\" style=\"background:#f7f7f9;color:#444;font-weight:600;text-align:center;font-size:10px;text-transform:uppercase;white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;min-width:60px;\">Present</th>");
        html.append("<th class=\"col-count\" style=\"background:#f7f7f9;color:#444;font-weight:600;text-align:center;font-size:10px;text-transform:uppercase;white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;min-width:45px;\">Late</th>");
        html.append("<th class=\"col-count\" style=\"background:#f7f7f9;color:#444;font-weight:600;text-align:center;font-size:10px;text-transform:uppercase;white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;min-width:55px;\">Absent</th>");
        html.append("<th class=\"col-count\" style=\"background:#f7f7f9;color:#444;font-weight:600;text-align:center;font-size:10px;text-transform:uppercase;white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;min-width:60px;\">Excused</th>");
        html.append("<th class=\"col-status\" style=\"background:#f7f7f9;color:#444;font-weight:600;text-align:center;font-size:10px;text-transform:uppercase;white-space:nowrap;padding:14px 10px;border-bottom:2px solid #86141e;min-width:120px;\">Status</th>");
        html.append("</tr></thead><tbody>");

        if (tableModel.getRowCount() == 0) {
            html.append("<tr><td colspan=\"7\" style=\"text-align:center;padding:28px;color:#999999;\">");
            html.append("No attendance data available.</td></tr>");
        } else {
            for (int row : sortedModelRowsByStudentName()) {
                appendEmailRow(html, row);
            }
        }

        html.append("</tbody></table>");
        html.append("</td></tr>");
        // Footer
        html.append("<tr><td style=\"background:#f7f7f9;border-top:3px solid #86141e;padding:24px 36px;text-align:center;\">");
        html.append("<div style=\"color:#86141e;font-size:16px;font-weight:700;margin-bottom:8px;\">SEAttendance</div>");
        html.append("<div style=\"color:#888;font-size:12px;line-height:1.7;\">This is an automated report generated by the SEAttendance System.");
        html.append("<br>For questions, please contact your department administrator.");
        html.append("<br>Please do not reply to this email.</div>");
        html.append("</td></tr>");
        // Close card table and wrap table
        html.append("</table>");
        html.append("</td></tr></table>");
        html.append("</body></html>");
        return html.toString();
    }

    private void appendSummaryCard(StringBuilder html, String label, String value, String styleClass) {
        String bg, borderColor, valueColor;
        switch (styleClass) {
            case "metric-present": bg = "#f0faf3"; borderColor = "#168b35"; valueColor = "#168b35"; break;
            case "metric-late":    bg = "#fef8ee"; borderColor = "#d07a00"; valueColor = "#d07a00"; break;
            case "metric-absent":  bg = "#fef2f2"; borderColor = "#9f1824"; valueColor = "#9f1824"; break;
            case "metric-excused": bg = "#f0f4f8"; borderColor = "#5b7a91"; valueColor = "#5b7a91"; break;
            case "metric-warning": bg = "#fff8e1"; borderColor = "#d07a00"; valueColor = "#d07a00"; break;
            case "metric-fda":     bg = "#fdf0f1"; borderColor = "#c41e2a"; valueColor = "#c41e2a"; break;
            default:               bg = "#f7f7f9"; borderColor = "#e8e8e8"; valueColor = "#333"; break;
        }
        html.append("<td class=\"summary-cell\" style=\"width:16.66%;vertical-align:top;\">");
        html.append("<div class=\"metric ").append(styleClass).append("\" style=\"border-radius:8px;padding:18px 8px;text-align:center;border:1px solid #e8e8e8;background:").append(bg).append(";border-top:4px solid ").append(borderColor).append(";\">");
        html.append("<span class=\"metric-value\" style=\"display:block;font-size:32px;line-height:1;font-weight:700;margin-bottom:6px;color:").append(valueColor).append(";\">")
                .append(escapeHtml(value)).append("</span>");
        html.append("<span class=\"metric-label\" style=\"display:block;font-size:10px;color:#666;font-weight:600;text-transform:uppercase;\">")
                .append(label).append("</span>");
        html.append("</div></td>");
    }

    private java.util.List<Integer> sortedModelRowsByStudentName() {
        java.util.List<Integer> rows = new java.util.ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            rows.add(row);
        }
        rows.sort((firstRow, secondRow) -> compareStudentNames(
                tableModel.getValueAt(firstRow, 2),
                tableModel.getValueAt(secondRow, 2)));
        return rows;
    }

    private int compareStudentNames(Object first, Object second) {
        return String.CASE_INSENSITIVE_ORDER.compare(sortableText(first), sortableText(second));
    }

    private String sortableText(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String getEmailReportSubject() {
        if (selectedClass == null) {
            return displayLabel == null ? "" : displayLabel;
        }
        return selectedClass.sectionName + " - " + selectedClass.subjectCode + " " + selectedClass.subjectName;
    }

    private String getEmailInstructorName() {
        return selectedClass == null ? "" : selectedClass.teacherName;
    }

    private void appendEmailRow(StringBuilder html, int row) {
        html.append("<tr>");
        appendTableCell(html, "", tableModel.getValueAt(row, 2), "col-name");
        appendTableCell(html, "ID", tableModel.getValueAt(row, 1), "col-id");
        appendTableCell(html, "Present", tableModel.getValueAt(row, 3), "col-count");
        appendTableCell(html, "Late", tableModel.getValueAt(row, 4), "col-count");
        appendTableCell(html, "Absent", tableModel.getValueAt(row, 5), "col-count");
        appendTableCell(html, "Excused", tableModel.getValueAt(row, 6), "col-count");
        String status = formatEmailStatus(tableModel.getValueAt(row, 7));
        String badgeStyle = getStatusBadgeInlineStyle(status);
        html.append("<td class=\"col-status\" style=\"padding:14px 10px;vertical-align:middle;border-bottom:1px solid #f0f0f0;text-align:center;\">");
        html.append("<span class=\"mobile-label\" style=\"display:none;font-weight:600;color:#888;font-size:11px;text-transform:uppercase;margin-right:6px;\">Status: </span>");
        html.append("<span class=\"status-badge ").append(statusClass(status)).append("\" style=\"").append(badgeStyle).append("\">");
        html.append(escapeHtml(status)).append("</span></td>");
        html.append("</tr>");
    }

    private void appendTableCell(StringBuilder html, String label, Object value, String styleClass) {
        String align = "col-count".equals(styleClass) ? "center" : "left";
        html.append("<td class=\"").append(styleClass).append("\" style=\"padding:14px 10px;vertical-align:middle;border-bottom:1px solid #f0f0f0;color:#333;text-align:").append(align).append(";white-space:nowrap;\">");
        if (!label.isEmpty()) {
            html.append("<span class=\"mobile-label\" style=\"display:none;font-weight:600;color:#888;font-size:11px;text-transform:uppercase;margin-right:6px;\">").append(label).append(": </span>");
        }
        html.append(escapeHtml(value)).append("</td>");
    }

    private String getStatusBadgeInlineStyle(String status) {
        String bg, color;
        if (isFdaStatus(status))          { bg = "#fce4ec"; color = "#c62828"; }
        else if (isWarningStatus(status)) { bg = "#fff8e1"; color = "#e65100"; }
        else                              { bg = "#e8f5e9"; color = "#2e7d32"; }
        return "display:inline-block;min-width:60px;padding:6px 10px;border-radius:20px;font-weight:600;font-size:11px;text-align:center;text-transform:uppercase;letter-spacing:0.3px;background:" + bg + ";color:" + color + ";";
    }

    private String formatEmailStatus(Object value) {
        String status = value == null ? "" : value.toString();
        if ("WARNING".equalsIgnoreCase(status)) {
            return "Warning";
        }
        return status;
    }

    private String statusClass(String status) {
        if (isFdaStatus(status)) {
            return "status-fda";
        }
        if (isWarningStatus(status)) {
            return "status-warning";
        }
        String value = status == null ? "" : status.toLowerCase(Locale.ROOT);
        if (value.contains("late")) {
            return "status-late";
        }
        if (value.contains("absent")) {
            return "status-absent";
        }
        if (value.contains("excused")) {
            return "status-excused";
        }
        return "status-good";
    }

    private boolean isFdaStatus(Object status) {
        if (status == null) {
            return false;
        }

        String value = status.toString();
        return "FDA".equalsIgnoreCase(value);
    }

    private boolean isWarningStatus(Object status) {
        if (status == null) {
            return false;
        }

        String value = status.toString();
        return "Warning".equalsIgnoreCase(value) || "WARNING".equalsIgnoreCase(value);
    }

    private String escapeHtml(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString();
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Font font(int style, int size) {
        return new Font("Segoe UI", style, Math.max(10, s(size)));
    }

    private Color blend(Color from, Color to, double ratio) {
        double clamped = Math.max(0, Math.min(1, ratio));
        int red = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int green = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int blue = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int alpha = (int) Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(red, green, blue, alpha);
    }

    private enum ReportIcon {
        PRESENT("/images/seatendance icons/present.png"),
        LATE("/images/seatendance icons/late.png"),
        ABSENT("/images/seatendance icons/absent.png"),
        EXCUSED("/images/seatendance icons/excused.png"),
        WARNING("/images/seatendance icons/warning.png"),
        FDA("/images/seatendance icons/fda.png");

        private final String path;
        private Image image;

        ReportIcon(String path) {
            this.path = path;
        }

        private Image image() {
            if (image == null) {
                java.net.URL resource = AttendanceReport.class.getResource(path);
                if (resource != null) {
                    image = new ImageIcon(resource).getImage();
                }
            }
            return image;
        }
    }

    private final class IconImage extends JComponent {
        private final ReportIcon icon;
        private final int size;

        private IconImage(ReportIcon icon, int size) {
            this.icon = icon;
            this.size = size;
            Dimension dimension = new Dimension(s(size), s(size));
            setPreferredSize(dimension);
            setMinimumSize(dimension);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Image image = icon.image();
            if (image == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(new ImageIcon(image.getScaledInstance(s(size), s(size), Image.SCALE_SMOOTH)).getImage(), 0, 0,
                    this);
            g2.dispose();
        }
    }

    private final class ReportTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Color rowBg = row % 2 == 0 ? TABLE_ROW_BG : TABLE_ROW_ALT_BG;

            if (column == 7) {
                return new StatusPill(value == null ? "" : value.toString(), rowBg);
            }

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 0) {
                setText(String.valueOf(row + 1));
            }
            setBackground(rowBg);
            setForeground(TEXT);
            setFont(font(Font.PLAIN, 14));
            setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 1, TABLE_ROW_LINE) {
                        @Override
                        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                            g.setColor(TABLE_ROW_LINE);
                            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
                            g.setColor(TABLE_VERTICAL_LINE);
                            g.drawLine(x + width - 1, y + s(12), x + width - 1, y + height - s(12));
                        }
                    },
                    new EmptyBorder(s(10), s(16), s(10), s(12))));
            setHorizontalAlignment(column >= 3 && column <= 6 ? SwingConstants.CENTER : SwingConstants.LEFT);
            return this;
        }
    }

    private final class StatusGlyph extends JComponent {
        private Color color;
        private boolean checked;

        private StatusGlyph(Color color, boolean checked) {
            this.color = color;
            this.checked = checked;
            Dimension size = new Dimension(s(16), s(16));
            setPreferredSize(size);
            setMinimumSize(size);
            setOpaque(false);
        }

        private void setState(Color color, boolean checked) {
            this.color = color;
            this.checked = checked;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int diameter = Math.min(getWidth(), getHeight());
            int inset = Math.max(1, s(1));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 32));
            g2.fillOval(inset, inset, diameter - inset * 2, diameter - inset * 2);

            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(1.5f, s(2)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (checked) {
                g2.drawLine(s(5), s(8), s(7), s(10));
                g2.drawLine(s(7), s(10), s(12), s(5));
            } else {
                g2.drawLine(s(5), s(5), s(11), s(11));
                g2.drawLine(s(11), s(5), s(5), s(11));
            }
            g2.dispose();
        }
    }

    private final class HeaderRenderer extends JComponent implements TableCellRenderer {
        private String text = "";
        private boolean centered;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            int modelColumn = table.convertColumnIndexToModel(column);
            text = value == null ? "" : value.toString();
            centered = modelColumn >= 3 && modelColumn <= 7 || "Status".equals(text);

            setBackground(TABLE_HEADER_BG);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(TABLE_HEADER_BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(TEXT);
            g2.setFont(font(Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int x = centered ? (getWidth() - textWidth) / 2 : s(16);
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, Math.max(s(6), x), y);

            g2.setColor(TABLE_ROW_LINE);
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            g2.setColor(TABLE_VERTICAL_LINE);
            g2.drawLine(getWidth() - 1, s(10), getWidth() - 1, getHeight() - s(10));
            g2.dispose();
        }
    }

    private final class StatusPill extends JComponent {
        private final String status;
        private final Color rowBg;

        private StatusPill(String status, Color rowBg) {
            this.status = status == null ? "" : status;
            this.rowBg = rowBg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(rowBg);
            g2.fillRect(0, 0, getWidth(), getHeight());

            boolean fda = isFdaStatus(status);
            boolean warning = isWarningStatus(status);
            Color accent = fda ? PINK : warning ? ORANGE : GREEN;
            Color fill = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 34);
            String text = fda ? "FDA" : warning ? "WARNING" : "GOOD STANDING";
            g2.setFont(font(Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int pillWidth = Math.max(s(124), fm.stringWidth(text) + s(26));
            int pillHeight = s(30);
            int x = (getWidth() - pillWidth) / 2;
            int y = (getHeight() - pillHeight) / 2;

            g2.setColor(fill);
            g2.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight);
            g2.setColor(accent);
            g2.drawString(text, x + (pillWidth - fm.stringWidth(text)) / 2,
                    y + (pillHeight - fm.getHeight()) / 2 + fm.getAscent());
            g2.setColor(TABLE_ROW_LINE);
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            g2.setColor(TABLE_VERTICAL_LINE);
            g2.drawLine(getWidth() - 1, s(12), getWidth() - 1, getHeight() - s(12));
            g2.dispose();
        }
    }

    private final class StatusRowFilter extends RowFilter<DefaultTableModel, Integer> {
        private final String status;

        private StatusRowFilter(String status) {
            this.status = status;
        }

        @Override
        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
            if ("FDA".equalsIgnoreCase(status)) {
                return isFdaStatus(entry.getStringValue(7));
            }
            if ("Warning".equalsIgnoreCase(status)) {
                return isWarningStatus(entry.getStringValue(7));
            }
            return status.equalsIgnoreCase(entry.getStringValue(7));
        }
    }

    private final class HintTextField extends JTextField {
        private final String hint;

        private HintTextField(String hint) {
            this.hint = hint;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, s(8), s(8));
            g2.dispose();
            super.paintComponent(g);

            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D hintGraphics = (Graphics2D) g.create();
                hintGraphics.setColor(MUTED);
                hintGraphics.setFont(getFont());
                FontMetrics fm = hintGraphics.getFontMetrics();
                Insets insets = getInsets();
                hintGraphics.drawString(hint, insets.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                hintGraphics.dispose();
            }
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CONTROL_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, s(8), s(8));
            g2.dispose();
        }
    }

    private final class StyledComboBox<E> extends JComboBox<E> {
        private StyledComboBox(E[] items) {
            super(items);
            setOpaque(false);
            setBorder(new EmptyBorder(0, s(12), 0, s(28)));
            setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(CONTROL_BG);
                    g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                    g2.dispose();
                }

                @Override
                protected JButton createArrowButton() {
                    JButton button = new JButton();
                    button.setOpaque(false);
                    button.setContentAreaFilled(false);
                    button.setBorderPainted(false);
                    button.setFocusPainted(false);
                    button.setPreferredSize(new Dimension(s(24), 0));
                    return button;
                }
            });
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setBorder(new EmptyBorder(s(7), s(10), s(7), s(10)));
                    setBackground(isSelected ? CONTROL_HOVER_BG : CONTROL_BG);
                    setForeground(TEXT);
                    setOpaque(index >= 0);
                    return this;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, s(8), s(8));
            g2.dispose();
            super.paintComponent(g);

            Graphics2D arrow = (Graphics2D) g.create();
            arrow.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            arrow.setColor(MUTED);
            int centerX = getWidth() - s(20);
            int centerY = getHeight() / 2;
            arrow.setStroke(new BasicStroke(Math.max(1, s(2)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            arrow.drawLine(centerX - s(4), centerY - s(2), centerX, centerY + s(3));
            arrow.drawLine(centerX, centerY + s(3), centerX + s(4), centerY - s(2));
            arrow.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CONTROL_BORDER);
            float stroke = Math.max(1f, s(1));
            float half = stroke / 2f;
            g2.setStroke(new BasicStroke(stroke));
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(
                    half,
                    half,
                    getWidth() - stroke,
                    getHeight() - stroke,
                    s(8),
                    s(8)));
            g2.dispose();
        }
    }

    private final class DarkScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(67, 79, 90);
            trackColor = new Color(21, 27, 32);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(trackColor);
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            int inset = s(2);
            g2.fillRoundRect(
                    thumbBounds.x + inset,
                    thumbBounds.y + inset,
                    thumbBounds.width - inset * 2,
                    thumbBounds.height - inset * 2,
                    s(8),
                    s(8));
            g2.dispose();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            return button;
        }
    }

    private final class HoverActionButton extends JButton {
        private final Color fillColor;
        private final Color borderColor;
        private final Timer hoverTimer;
        private boolean hovered;
        private double hoverProgress;

        private HoverActionButton(String text, Color fillColor, Color borderColor) {
            super(text);
            this.fillColor = fillColor;
            this.borderColor = borderColor;

            setForeground(TEXT);
            setFont(font(Font.BOLD, 15));
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(0, s(14), 0, s(14)));

            hoverTimer = new Timer(15, e -> updateHoverAnimation());
            hoverTimer.setCoalesce(true);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hovered = true;
                    hoverTimer.start();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
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

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (hoverTimer != null) {
                hoverTimer.start();
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double progress = isEnabled() ? hoverProgress : 0.0;
            int inset = (int) Math.round(s(3) * (1.0 - progress));
            int width = getWidth() - inset * 2;
            int height = getHeight() - inset * 2;
            Color disabledFill = new Color(55, 59, 64);
            Color currentFill = isEnabled()
                    ? blend(fillColor, Color.WHITE, 0.11 * progress)
                    : disabledFill;
            Color currentBorder = isEnabled()
                    ? blend(borderColor, Color.WHITE, 0.18 * progress)
                    : BORDER;
            float stroke = Math.max(1f, s(1));
            float half = stroke / 2f;

            g2.setColor(currentFill);
            g2.fillRoundRect(inset, inset, width, height, s(6), s(6));
            g2.setColor(currentBorder);
            g2.setStroke(new BasicStroke(stroke));
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(
                    inset + half,
                    inset + half,
                    width - stroke,
                    height - stroke,
                    s(6),
                    s(6)));

            g2.setFont(getFont().deriveFont((float) (getFont().getSize2D() + Math.max(1, s(1)) * progress)));
            g2.setColor(isEnabled() ? TEXT : MUTED);
            FontMetrics fm = g2.getFontMetrics();
            String label = getText();
            int textX = inset + (width - fm.stringWidth(label)) / 2;
            int textY = inset + (height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, textX, textY);
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
        public Insets getBorderInsets(Component c) {
            int inset = Math.max(1, s(1));
            return new Insets(inset, inset, inset, inset);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            int inset = Math.max(1, s(1));
            insets.set(inset, inset, inset, inset);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float stroke = Math.max(1f, s(1));
            float half = stroke / 2f;
            g2.setStroke(new BasicStroke(stroke));
            g2.setColor(color);
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(
                    x + half,
                    y + half,
                    width - stroke,
                    height - stroke,
                    s(radius),
                    s(radius)));
            g2.dispose();
        }
    }

    private final class VerticalRule extends JComponent {
        private VerticalRule() {
            setPreferredSize(new Dimension(s(1), s(18)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(BORDER);
            g.drawLine(0, 0, 0, getHeight());
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
            g2.setStroke(new BasicStroke(s(borderWidth)));
            int inset = Math.max(1, s(borderWidth));
            g2.drawRoundRect(inset / 2, inset / 2, getWidth() - inset, getHeight() - inset, s(radius), s(radius));
            g2.dispose();
        }
    }
}
