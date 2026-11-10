package com.seatendance;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class DatabaseEditor extends ScaledPanel {

    private static final Color CONTENT_BG = new Color(34, 38, 42);
    private static final Color CARD_BG = new Color(20, 25, 28);
    private static final Color CONTROL_BG = new Color(36, 41, 46);
    private static final Color BORDER = new Color(63, 70, 78);
    private static final Color TABLE_LINE = new Color(43, 52, 60);
    private static final Color TEXT = Color.WHITE;
    private static final Color MUTED = new Color(174, 182, 193);
    private static final Color RED = new Color(190, 40, 49);
    private static final Color GREEN_FB = new Color(80, 220, 80);
    private static final Color RED_FB = new Color(220, 80, 80);

    private final DatabaseHandler db;
    private final String role;
    private final RFIDScanner rfidScanner;

    public DatabaseEditor(DatabaseHandler db, double scale, String role, RFIDScanner rfidScanner) {
        super(scale);
        this.db = db;
        this.role = role;
        this.rfidScanner = rfidScanner;
        setBackground(CONTENT_BG);
        setLayout(new BorderLayout());

        CardLayout cardLayout = new CardLayout();
        JPanel nav = new JPanel(cardLayout);
        nav.setBackground(CONTENT_BG);
        add(nav, BorderLayout.CENTER);

        nav.add(new ChoicePanel(scale, nav), "choose");

        if (role.equals("admin")) {
            nav.add(new UnifiedCrudPanel(scale, nav, "Course / Program", this::buildCourseUnified), "course");
            nav.add(new UnifiedCrudPanel(scale, nav, "Teacher", this::buildTeacherUnified), "teacher");
            nav.add(new UnifiedCrudPanel(scale, nav, "Subject", this::buildSubjectUnified), "subject");
            nav.add(new UnifiedCrudPanel(scale, nav, "Section", this::buildSectionUnified), "section");
            nav.add(new EnrollmentPanel(scale, nav), "enrollment");
            nav.add(new SectionSubjectPanel(scale, nav), "sectionSubject");
            nav.add(new SubjectIdPanel(scale, nav), "subjectIdPanel");
        }
        if (role.equals("admin") || role.equals("teacher")) {
            nav.add(new UnifiedCrudPanel(scale, nav, "Student", this::buildStudentUnified), "student");
        }
        if (role.equals("student")) {
            nav.add(new StudentSelfEditPanel(scale, nav), "student");
        }

        cardLayout.show(nav, "choose");
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void bindRFID(JTextField uidField) {
        if (rfidScanner == null)
            return;
        uidField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                rfidScanner.setOnUIDSScanned(uid -> {
                    SwingUtilities.invokeLater(() -> uidField.setText(uid));
                });
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                rfidScanner.setOnUIDSScanned(null);
            }
        });
    }

    private static void showCard(JPanel nav, String card) {
        ((CardLayout) nav.getLayout()).show(nav, card);
        nav.revalidate();
        nav.repaint();
    }

    private Font font(int style, int size) {
        return new Font("Segoe UI", style, Math.max(10, s(size)));
    }

    private JLabel mkLabel(String text, int size, boolean bold) {
        JLabel l = new JLabel(text);
        l.setForeground(bold ? TEXT : MUTED);
        l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        return l;
    }

    private JTextField mkField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(font(Font.PLAIN, 14));
        f.setBackground(CONTROL_BG);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(s(4), s(8), s(4), s(8))));
        return f;
    }

    private JPasswordField mkPassword(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setFont(font(Font.PLAIN, 14));
        f.setBackground(CONTROL_BG);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(s(4), s(8), s(4), s(8))));
        return f;
    }

    private <T> JComboBox<T> mkCombo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setFont(font(Font.PLAIN, 14));
        c.setBackground(CONTROL_BG);
        c.setForeground(TEXT);
        c.setFocusable(false);
        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
                setBackground(isSelected ? RED : CONTROL_BG);
                setForeground(TEXT);
                setBorder(new EmptyBorder(s(4), s(8), s(4), s(8)));
                return this;
            }
        });
        return c;
    }

    private JLabel mkFeedback() {
        JLabel l = new JLabel(" ");
        l.setFont(font(Font.ITALIC, 13));
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private JButton mkBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(font(Font.PLAIN, 14));
        b.setBackground(CONTROL_BG);
        b.setForeground(TEXT);
        b.setFocusPainted(false);
        b.setBorderPainted(true);
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(s(6), s(14), s(6), s(14))));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton mkPrimaryBtn(String text) {
        JButton b = mkBtn(text);
        b.setBackground(RED);
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 60, 70), 1),
                new EmptyBorder(s(6), s(14), s(6), s(14))));
        return b;
    }

    private JButton mkDangerBtn(String text) {
        JButton b = mkBtn(text);
        b.setForeground(RED_FB);
        return b;
    }

    private static void feedback(JLabel lbl, boolean ok, String msg) {
        lbl.setText(msg);
        lbl.setForeground(ok ? GREEN_FB : RED_FB);
    }

    private JPanel row(String labelText, JComponent field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, s(10), s(4)));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(MUTED);
        lbl.setFont(font(Font.PLAIN, 14));
        lbl.setPreferredSize(new Dimension(s(150), s(24)));
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(lbl);
        p.add(field);
        return p;
    }

    /** Build a styled, non-editable table from a ResultSet. */
    private JTable tableFromResultSet(ResultSet rs, DefaultTableModel model) {
        model.setRowCount(0);
        model.setColumnCount(0);
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++)
                model.addColumn(meta.getColumnLabel(i));
            while (rs.next()) {
                Object[] r = new Object[cols];
                for (int i = 1; i <= cols; i++)
                    r[i - 1] = rs.getObject(i);
                model.addRow(r);
            }
        } catch (SQLException ignored) {
        }

        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table.setBackground(CARD_BG);
        table.setForeground(TEXT);
        table.setFont(font(Font.PLAIN, 13));
        table.setRowHeight(s(28));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(CONTROL_BG);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(CARD_BG);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setFont(font(Font.BOLD, 13));
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, TABLE_LINE));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBackground(isSelected ? CONTROL_BG : (row % 2 == 0 ? CARD_BG : new Color(24, 30, 34)));
                setForeground(TEXT);
                setBorder(new EmptyBorder(0, s(8), 0, s(8)));
                return this;
            }
        });
        return table;
    }

    /** Reusable live-table component that refreshes on demand. */
    private JScrollPane makeLiveTablePane(DefaultTableModel model) {
        // The table is built once; rows are updated via model.setRowCount(0) + addRow
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table.setBackground(CARD_BG);
        table.setForeground(TEXT);
        table.setFont(font(Font.PLAIN, 13));
        table.setRowHeight(s(28));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(CONTROL_BG);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(CARD_BG);
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setFont(font(Font.BOLD, 13));
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, TABLE_LINE));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                // Convert boolean/0/1 values to Yes/No for columns named is_* or has_*
                String colName = t.getColumnName(col);
                if (colName != null && (colName.startsWith("is_") || colName.startsWith("has_"))) {
                    if (value instanceof Boolean) {
                        value = ((Boolean) value) ? "Yes" : "No";
                    } else if (value instanceof Number) {
                        value = (((Number) value).intValue() != 0) ? "Yes" : "No";
                    }
                }
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBackground(isSelected ? CONTROL_BG : (row % 2 == 0 ? CARD_BG : new Color(24, 30, 34)));
                setForeground(TEXT);
                setBorder(new EmptyBorder(0, s(8), 0, s(8)));
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER, 1));
        scroll.setBackground(CARD_BG);
        scroll.getViewport().setBackground(CARD_BG);
        return scroll;
    }

    private JPanel centered(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    // =========================================================================
    // ITEM CLASSES
    // =========================================================================

    private static class SectionItem {
        final int id;
        final String name;

        SectionItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + "  [" + id + "]";
        }
    }

    private static class SubjectItem {
        final String code, course, display;

        SubjectItem(String code, String course, String name) {
            this.code = code;
            this.course = course;
            this.display = code + " — " + name + " (" + course + ")";
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private static class TeacherItem {
        final String id, display;

        TeacherItem(String id, String last, String first) {
            this.id = id;
            this.display = id + " — " + last + ", " + first;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    // =========================================================================
    // COMBO BUILDERS
    // =========================================================================

    private Object[] buildSectionPicker() {
        JComboBox<String> courseFilter = mkCombo(new String[0]);
        JComboBox<SectionItem> sectionCombo = mkCombo(new SectionItem[0]);

        Runnable rebuildSections = () -> {
            String course = (String) courseFilter.getSelectedItem();
            SectionItem sel = (SectionItem) sectionCombo.getSelectedItem();
            sectionCombo.removeAllItems();
            sectionCombo.addItem(new SectionItem(-1, "— None —"));
            if (course == null)
                return;
            try (ResultSet rs = db.getSectionsByCourse(course)) {
                if (rs != null)
                    while (rs.next())
                        sectionCombo.addItem(new SectionItem(rs.getInt("section_id"), rs.getString("section_name")));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if (sel != null)
                for (int i = 0; i < sectionCombo.getItemCount(); i++)
                    if (sectionCombo.getItemAt(i).id == sel.id) {
                        sectionCombo.setSelectedIndex(i);
                        break;
                    }
        };

        try (ResultSet rs = db.getCourses()) {
            if (rs != null)
                while (rs.next()) {
                    String c = rs.getString("course");
                    if (c != null)
                        courseFilter.addItem(c);
                }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        rebuildSections.run();
        courseFilter.addActionListener(e -> rebuildSections.run());
        courseFilter.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                String saved = (String) courseFilter.getSelectedItem();
                courseFilter.removeAllItems();
                try (ResultSet rs = db.getCourses()) {
                    if (rs != null)
                        while (rs.next()) {
                            String c = rs.getString("course");
                            if (c != null)
                                courseFilter.addItem(c);
                        }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                if (saved != null)
                    courseFilter.setSelectedItem(saved);
                rebuildSections.run();
            }

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });
        return new Object[] { courseFilter, sectionCombo };
    }

    private Object[] buildSubjectPickerWithCourseFilter() {
        JComboBox<String> courseFilter = mkCombo(new String[0]);
        JComboBox<SubjectItem> subjectCombo = mkCombo(new SubjectItem[0]);

        Runnable rebuildSubjects = () -> {
            String course = (String) courseFilter.getSelectedItem();
            SubjectItem sel = (SubjectItem) subjectCombo.getSelectedItem();
            subjectCombo.removeAllItems();
            if (course == null)
                return;
            try (ResultSet rs = db.getSubjectsByCourse(course)) {
                if (rs != null)
                    while (rs.next())
                        subjectCombo.addItem(new SubjectItem(rs.getString("subject_code"), rs.getString("course"),
                                rs.getString("subject_name")));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if (sel != null)
                for (int i = 0; i < subjectCombo.getItemCount(); i++)
                    if (subjectCombo.getItemAt(i).code.equals(sel.code)) {
                        subjectCombo.setSelectedIndex(i);
                        break;
                    }
        };

        try (ResultSet rs = db.getCourses()) {
            if (rs != null)
                while (rs.next())
                    courseFilter.addItem(rs.getString("course"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        rebuildSubjects.run();
        courseFilter.addActionListener(e -> rebuildSubjects.run());
        courseFilter.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                String saved = (String) courseFilter.getSelectedItem();
                courseFilter.removeAllItems();
                try (ResultSet rs = db.getCourses()) {
                    if (rs != null)
                        while (rs.next())
                            courseFilter.addItem(rs.getString("course"));
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                if (saved != null)
                    courseFilter.setSelectedItem(saved);
                rebuildSubjects.run();
            }

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });
        return new Object[] { courseFilter, subjectCombo };
    }

    private JComboBox<TeacherItem> buildTeacherCombo() {
        JComboBox<TeacherItem> combo = mkCombo(new TeacherItem[0]);
        Runnable load = () -> {
            TeacherItem sel = (TeacherItem) combo.getSelectedItem();
            combo.removeAllItems();
            try (ResultSet rs = db.getTeachers()) {
                if (rs != null)
                    while (rs.next())
                        combo.addItem(new TeacherItem(rs.getString("teacher_id"), rs.getString("last_name"),
                                rs.getString("first_name")));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if (sel != null)
                for (int i = 0; i < combo.getItemCount(); i++)
                    if (combo.getItemAt(i).id.equals(sel.id)) {
                        combo.setSelectedIndex(i);
                        break;
                    }
        };
        load.run();
        combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                load.run();
            }

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });
        return combo;
    }

    // =========================================================================
    // UNIFIED CRUD PANEL — form + live table in one panel
    // =========================================================================

    /** Provides the form area and a refresh callback that reloads the table. */
    interface UnifiedFormBuilder {
        /** @return [0] = form JPanel, [1] = Runnable that reloads the table model */
        Object[] build(DefaultTableModel tableModel, JLabel feedback);
    }

    /**
     * Single panel per entity:
     * TOP — card containing the full Add / Edit / Delete form
     * BOTTOM — always-visible live table (auto-loads on open, refreshes after
     * mutations)
     */
    private class UnifiedCrudPanel extends ScaledPanel {
        UnifiedCrudPanel(double scale, JPanel nav, String title, UnifiedFormBuilder builder) {
            super(scale);
            setLayout(new BorderLayout(0, s(14)));
            setBackground(CONTENT_BG);
            setBorder(new EmptyBorder(s(24), s(36), s(24), s(36)));

            // ---- title ----
            JLabel titleLbl = new JLabel(title);
            titleLbl.setForeground(TEXT);
            titleLbl.setFont(font(Font.BOLD, 22));

            // ---- live table ----
            DefaultTableModel tableModel = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            JScrollPane tableScroll = makeLiveTablePane(tableModel);
            tableScroll.setPreferredSize(new Dimension(0, s(220)));

            JLabel refreshIcon = mkLabel("↻ Refresh", 12, false);
            refreshIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // ---- feedback ----
            JLabel feedback = mkFeedback();

            // ---- build form ----
            Object[] built = builder.build(tableModel, feedback);
            JPanel formPanel = (JPanel) built[0];
            Runnable refresh = (Runnable) built[1];

            // Attach refresh to the label click
            refreshIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    refresh.run();
                }
            });

            // ---- form card ----
            JPanel formCard = new JPanel(new BorderLayout(0, s(8)));
            formCard.setBackground(CARD_BG);
            formCard.setBorder(new CompoundBorder(
                    new LineBorder(BORDER, 1),
                    new EmptyBorder(s(20), s(24), s(20), s(24))));
            formCard.add(formPanel, BorderLayout.CENTER);

            JPanel fbBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            fbBar.setOpaque(false);
            fbBar.add(feedback);
            formCard.add(fbBar, BorderLayout.SOUTH);

            // ---- table section ----
            JPanel tableHeader = new JPanel(new BorderLayout());
            tableHeader.setOpaque(false);
            JLabel tableLbl = mkLabel("All Records", 13, true);
            tableHeader.add(tableLbl, BorderLayout.WEST);
            tableHeader.add(refreshIcon, BorderLayout.EAST);

            JPanel tableSection = new JPanel(new BorderLayout(0, s(6)));
            tableSection.setOpaque(false);
            tableSection.add(tableHeader, BorderLayout.NORTH);
            tableSection.add(tableScroll, BorderLayout.CENTER);

            // ---- back button ----
            JButton backBtn = mkBtn("← Back");
            backBtn.addActionListener(e -> showCard(nav, "choose"));
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            footer.setOpaque(false);
            footer.add(backBtn);

            // ---- header ----
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(titleLbl, BorderLayout.WEST);
            header.add(footer, BorderLayout.EAST);

            // ---- layout: form top, table bottom ----
            JPanel center = new JPanel(new BorderLayout(0, s(14)));
            center.setOpaque(false);
            center.add(formCard, BorderLayout.NORTH);
            center.add(tableSection, BorderLayout.CENTER);

            add(header, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);

            // Initial load
            refresh.run();
        }
    }

    // =========================================================================
    // UNIFIED FORM BUILDERS
    // =========================================================================

    // ---- COURSE ----
    private Object[] buildCourseUnified(DefaultTableModel tableModel, JLabel feedback) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Fields
        JTextField nameField = mkField(22), codeField = mkField(12);

        // Buttons
        JButton addBtn = mkPrimaryBtn("Add");
        JButton updateBtn = mkBtn("Update Code");
        JButton delBtn = mkDangerBtn("Delete");
        JButton clearBtn = mkBtn("Clear");

        // State: track currently selected course name for edit/delete
        String[] selectedName = { null };

        Runnable refresh = () -> {
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            ResultSet rs = db.getCourses();
            if (rs == null)
                return;
            try {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                if (tableModel.getColumnCount() == 0)
                    for (int i = 1; i <= cols; i++)
                        tableModel.addColumn(meta.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 1; i <= cols; i++)
                        row[i - 1] = rs.getObject(i);
                    tableModel.addRow(row);
                }
            } catch (SQLException ignored) {
            }
        };

        // Populate fields from name search
        JButton findBtn = mkBtn("Load");
        findBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                feedback(feedback, false, "Enter a course name or code to load.");
                return;
            }
            try (ResultSet rs = db.getCourses()) {
                boolean found = false;
                if (rs != null)
                    while (rs.next()) {
                        if (rs.getString("course").equalsIgnoreCase(name)
                                || rs.getString("course_code").equalsIgnoreCase(name)) {
                            nameField.setText(rs.getString("course"));
                            codeField.setText(rs.getString("course_code"));
                            selectedName[0] = rs.getString("course");
                            found = true;
                            break;
                        }
                    }
                feedback(feedback, found, found ? "Loaded — edit fields then click Update or Delete." : "Not found.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim(), code = codeField.getText().trim();
            if (name.isEmpty() || code.isEmpty()) {
                feedback(feedback, false, "Both fields required.");
                return;
            }
            boolean ok = db.addCourse(name, code);
            feedback(feedback, ok, ok ? "Course added." : "Failed — duplicate or DB error.");
            if (ok) {
                nameField.setText("");
                codeField.setText("");
                selectedName[0] = null;
                refresh.run();
            }
        });

        updateBtn.addActionListener(e -> {
            if (selectedName[0] == null) {
                feedback(feedback, false, "Load a course first.");
                return;
            }
            boolean ok = db.updateCourse(selectedName[0], codeField.getText().trim());
            feedback(feedback, ok, ok ? "Updated." : "Failed.");
            if (ok)
                refresh.run();
        });

        delBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                feedback(feedback, false, "Enter a course name.");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Delete \"" + name + "\"? May cascade to sections.",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;
            boolean ok = db.deleteCourse(name);
            feedback(feedback, ok, ok ? "Deleted." : "Not found.");
            if (ok) {
                nameField.setText("");
                codeField.setText("");
                selectedName[0] = null;
                refresh.run();
            }
        });

        clearBtn.addActionListener(e -> {
            nameField.setText("");
            codeField.setText("");
            selectedName[0] = null;
            feedback.setText(" ");
        });

        p.add(row("Course Name:", nameField));
        p.add(row("Course Code:", codeField));
        p.add(Box.createVerticalStrut(s(10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
        btnRow.setOpaque(false);
        btnRow.add(addBtn);
        btnRow.add(findBtn);
        btnRow.add(updateBtn);
        btnRow.add(delBtn);
        btnRow.add(clearBtn);
        p.add(btnRow);

        return new Object[] { p, refresh };
    }

    // ---- TEACHER ----
    private Object[] buildTeacherUnified(DefaultTableModel tableModel, JLabel feedback) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JTextField uidField = mkField(10), idField = mkField(14), firstField = mkField(20),
                lastField = mkField(20), miField = mkField(4), emailField = mkField(26);
        JPasswordField pwField = mkPassword(20);

        bindRFID(uidField);

        JButton addBtn = mkPrimaryBtn("Add");
        JButton findBtn = mkBtn("Load by ID");
        JButton updateBtn = mkBtn("Update");
        JButton delBtn = mkDangerBtn("Delete");
        JButton clearBtn = mkBtn("Clear");

        String[] loadedId = { null };

        Runnable refresh = () -> {
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            ResultSet rs = db.getTeachers();
            if (rs == null)
                return;
            try {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                if (tableModel.getColumnCount() == 0)
                    for (int i = 1; i <= cols; i++)
                        tableModel.addColumn(meta.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 1; i <= cols; i++)
                        row[i - 1] = rs.getObject(i);
                    tableModel.addRow(row);
                }
            } catch (SQLException ignored) {
            }
        };

        findBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                feedback(feedback, false, "Enter a Teacher ID.");
                return;
            }
            try (ResultSet rs = db.getTeachers()) {
                boolean found = false;
                if (rs != null)
                    while (rs.next()) {
                        if (rs.getString("teacher_id").equalsIgnoreCase(id)) {
                            firstField.setText(rs.getString("first_name"));
                            lastField.setText(rs.getString("last_name"));
                            String mi = rs.getString("middle_initial");
                            miField.setText(mi == null ? "" : mi);
                            emailField.setText(rs.getString("email") != null ? rs.getString("email") : "");
                            loadedId[0] = id;
                            found = true;
                            break;
                        }
                    }
                feedback(feedback, found, found ? "Loaded — edit then Update or Delete." : "Not found.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        addBtn.addActionListener(e -> {
            String uid = uidField.getText().trim(), id = idField.getText().trim(),
                    first = firstField.getText().trim(), last = lastField.getText().trim(),
                    mi = miField.getText().trim(), email = emailField.getText().trim(),
                    pw = new String(pwField.getPassword()).trim();
            if (uid.isEmpty() || id.isEmpty() || first.isEmpty() || last.isEmpty() || email.isEmpty() || pw.isEmpty()) {
                feedback(feedback, false, "All fields except Middle Initial are required.");
                return;
            }
            int userId = db.addUser(email, BCrypt.hashpw(pw, BCrypt.gensalt()), "teacher");
            if (userId == -1) {
                feedback(feedback, false, "Failed — email in use?");
                return;
            }
            boolean ok = db.addTeacher(uid, id, userId, first, last, mi.isEmpty() ? null : mi, email);
            if (!ok)
                db.deleteUser(userId);
            feedback(feedback, ok, ok ? "Teacher added." : "Failed — duplicate ID or UID?");
            if (ok) {
                uidField.setText("");
                idField.setText("");
                firstField.setText("");
                lastField.setText("");
                miField.setText("");
                emailField.setText("");
                pwField.setText("");
                loadedId[0] = null;
                refresh.run();
            }
        });

        updateBtn.addActionListener(e -> {
            if (loadedId[0] == null) {
                feedback(feedback, false, "Load a teacher first.");
                return;
            }
            boolean ok = db.updateTeacher(loadedId[0], firstField.getText().trim(), lastField.getText().trim(),
                    miField.getText().trim());
            feedback(feedback, ok, ok ? "Updated." : "Failed.");
            if (ok)
                refresh.run();
        });

        delBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                feedback(feedback, false, "Enter a Teacher ID.");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Delete teacher \"" + id + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;
            boolean ok = db.deleteTeacher(id);
            feedback(feedback, ok, ok ? "Deleted." : "Not found.");
            if (ok) {
                idField.setText("");
                firstField.setText("");
                lastField.setText("");
                miField.setText("");
                loadedId[0] = null;
                refresh.run();
            }
        });

        clearBtn.addActionListener(e -> {
            uidField.setText("");
            idField.setText("");
            firstField.setText("");
            lastField.setText("");
            miField.setText("");
            emailField.setText("");
            pwField.setText("");
            loadedId[0] = null;
            feedback.setText(" ");
        });

        // Layout: two rows of fields
        JPanel leftFields = new JPanel();
        leftFields.setOpaque(false);
        leftFields.setLayout(new BoxLayout(leftFields, BoxLayout.Y_AXIS));
        leftFields.add(row("UID (8 chars):", uidField));
        leftFields.add(row("Teacher ID:", idField));
        leftFields.add(row("First Name:", firstField));
        leftFields.add(row("Last Name:", lastField));

        JPanel rightFields = new JPanel();
        rightFields.setOpaque(false);
        rightFields.setLayout(new BoxLayout(rightFields, BoxLayout.Y_AXIS));
        rightFields.add(row("Middle Initial:", miField));
        rightFields.add(row("Email:", emailField));
        rightFields.add(row("Password:", pwField));

        JPanel fieldsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(20), 0));
        fieldsRow.setOpaque(false);
        fieldsRow.add(leftFields);
        fieldsRow.add(rightFields);
        p.add(fieldsRow);
        p.add(Box.createVerticalStrut(s(10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
        btnRow.setOpaque(false);
        btnRow.add(addBtn);
        btnRow.add(findBtn);
        btnRow.add(updateBtn);
        btnRow.add(delBtn);
        btnRow.add(clearBtn);
        p.add(btnRow);

        return new Object[] { p, refresh };
    }

    // ---- STUDENT ----
    private Object[] buildStudentUnified(DefaultTableModel tableModel, JLabel feedback) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JTextField uidField = mkField(10), sidField = mkField(16), firstField = mkField(20),
                lastField = mkField(20), miField = mkField(4), emailField = mkField(26);
        JCheckBox irregCheck = styledCheck("Irregular Student");
        Object[] picker = buildSectionPicker();
        @SuppressWarnings("unchecked")
        JComboBox<String> cf = (JComboBox<String>) picker[0];
        @SuppressWarnings("unchecked")
        JComboBox<SectionItem> sc = (JComboBox<SectionItem>) picker[1];

        bindRFID(uidField);

        JButton addBtn = mkPrimaryBtn("Add");
        JButton findBtn = mkBtn("Load by ID");
        JButton updateBtn = mkBtn("Update");
        JButton delBtn = mkDangerBtn("Delete");
        JButton clearBtn = mkBtn("Clear");

        Runnable refresh = () -> {
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            ResultSet rs = db.getStudents();
            if (rs == null)
                return;
            try {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                if (tableModel.getColumnCount() == 0)
                    for (int i = 1; i <= cols; i++)
                        tableModel.addColumn(meta.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 1; i <= cols; i++)
                        row[i - 1] = rs.getObject(i);
                    tableModel.addRow(row);
                }
            } catch (SQLException ignored) {
            }
        };

        findBtn.addActionListener(e -> {
            String id = sidField.getText().trim();
            if (id.isEmpty()) {
                feedback(feedback, false, "Enter a Student ID.");
                return;
            }
            try (ResultSet rs = db.getStudents()) {
                boolean found = false;
                if (rs != null)
                    while (rs.next()) {
                        if (rs.getString("student_id").equalsIgnoreCase(id)) {
                            firstField.setText(rs.getString("first_name"));
                            lastField.setText(rs.getString("last_name"));
                            String mi = rs.getString("middle_initial");
                            miField.setText(mi == null ? "" : mi);
                            emailField.setText(rs.getString("email"));
                            irregCheck.setSelected(rs.getBoolean("is_irregular"));
                            int secId = rs.getInt("section_id");
                            if (!rs.wasNull()) {
                                cf.setSelectedItem(rs.getString("course"));
                                for (int i = 0; i < sc.getItemCount(); i++)
                                    if (sc.getItemAt(i).id == secId) {
                                        sc.setSelectedIndex(i);
                                        break;
                                    }
                            } else {
                                sc.setSelectedIndex(0);
                            }
                            found = true;
                            break;
                        }
                    }
                feedback(feedback, found, found ? "Loaded — edit then Update or Delete." : "Not found.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        addBtn.addActionListener(e -> {
            String uid = uidField.getText().trim(), sid = sidField.getText().trim(),
                    first = firstField.getText().trim(), last = lastField.getText().trim(),
                    mi = miField.getText().trim(), email = emailField.getText().trim();
            if (uid.isEmpty() || sid.isEmpty() || first.isEmpty() || last.isEmpty() || email.isEmpty()) {
                feedback(feedback, false, "UID, Student ID, name, and email are required.");
                return;
            }
            SectionItem sel = (SectionItem) sc.getSelectedItem();
            boolean ok = db.addStudent(uid, sid, null, first, last, mi.isEmpty() ? null : mi, email,
                    (sel == null || sel.id == -1) ? null : sel.id, irregCheck.isSelected());
            feedback(feedback, ok, ok ? "Student added." : "Failed — duplicate UID, ID, or email?");
            if (ok) {
                uidField.setText("");
                sidField.setText("");
                firstField.setText("");
                lastField.setText("");
                miField.setText("");
                emailField.setText("");
                irregCheck.setSelected(false);
                refresh.run();
            }
        });

        updateBtn.addActionListener(e -> {
            SectionItem sel = (SectionItem) sc.getSelectedItem();
            String mi = miField.getText().trim();
            boolean ok = db.updateStudent(sidField.getText().trim(), null,
                    firstField.getText().trim(), lastField.getText().trim(),
                    mi.isEmpty() ? null : mi, emailField.getText().trim(),
                    (sel == null || sel.id == -1) ? null : sel.id, irregCheck.isSelected());
            feedback(feedback, ok, ok ? "Updated." : "Failed.");
            if (ok)
                refresh.run();
        });

        delBtn.addActionListener(e -> {
            String id = sidField.getText().trim();
            if (id.isEmpty()) {
                feedback(feedback, false, "Enter a Student ID.");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Delete student \"" + id + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;
            boolean ok = db.deleteStudent(id);
            feedback(feedback, ok, ok ? "Deleted." : "Not found.");
            if (ok) {
                sidField.setText("");
                firstField.setText("");
                lastField.setText("");
                refresh.run();
            }
        });

        clearBtn.addActionListener(e -> {
            uidField.setText("");
            sidField.setText("");
            firstField.setText("");
            lastField.setText("");
            miField.setText("");
            emailField.setText("");
            irregCheck.setSelected(false);
            feedback.setText(" ");
        });

        JPanel leftFields = new JPanel();
        leftFields.setOpaque(false);
        leftFields.setLayout(new BoxLayout(leftFields, BoxLayout.Y_AXIS));
        leftFields.add(row("UID:", uidField));
        leftFields.add(row("Student ID:", sidField));
        leftFields.add(row("First Name:", firstField));
        leftFields.add(row("Last Name:", lastField));

        JPanel rightFields = new JPanel();
        rightFields.setOpaque(false);
        rightFields.setLayout(new BoxLayout(rightFields, BoxLayout.Y_AXIS));
        rightFields.add(row("Middle Initial:", miField));
        rightFields.add(row("Email:", emailField));
        rightFields.add(row("Course:", cf));
        rightFields.add(row("Section:", sc));
        rightFields.add(row("", irregCheck));

        JPanel fieldsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(20), 0));
        fieldsRow.setOpaque(false);
        fieldsRow.add(leftFields);
        fieldsRow.add(rightFields);
        p.add(fieldsRow);
        p.add(Box.createVerticalStrut(s(10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
        btnRow.setOpaque(false);
        btnRow.add(addBtn);
        btnRow.add(findBtn);
        btnRow.add(updateBtn);
        btnRow.add(delBtn);
        btnRow.add(clearBtn);
        p.add(btnRow);

        return new Object[] { p, refresh };
    }

    // ---- SUBJECT ----
    private Object[] buildSubjectUnified(DefaultTableModel tableModel, JLabel feedback) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JTextField codeField = mkField(14), nameField = mkField(26);
        JComboBox<String> typeCombo = mkCombo(new String[] { "Professional", "Minor" });
        JComboBox<String> courseCombo = mkCombo(new String[0]);
        Runnable refreshCourse = () -> {
            String sel = (String) courseCombo.getSelectedItem();
            courseCombo.removeAllItems();
            try (ResultSet rs = db.getCourses()) {
                if (rs != null)
                    while (rs.next())
                        courseCombo.addItem(rs.getString("course"));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if (sel != null)
                courseCombo.setSelectedItem(sel);
        };
        refreshCourse.run();

        JButton addBtn = mkPrimaryBtn("Add");
        JButton findBtn = mkBtn("Load");
        JButton updateBtn = mkBtn("Update");
        JButton delBtn = mkDangerBtn("Delete");
        JButton clearBtn = mkBtn("Clear");

        Runnable refresh = () -> {
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            ResultSet rs = db.getSubjects();
            if (rs == null)
                return;
            try {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                if (tableModel.getColumnCount() == 0)
                    for (int i = 1; i <= cols; i++)
                        tableModel.addColumn(meta.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 1; i <= cols; i++)
                        row[i - 1] = rs.getObject(i);
                    tableModel.addRow(row);
                }
            } catch (SQLException ignored) {
            }
        };

        findBtn.addActionListener(e -> {
            String code = codeField.getText().trim(), course = (String) courseCombo.getSelectedItem();
            if (code.isEmpty() || course == null) {
                feedback(feedback, false, "Enter code and select course.");
                return;
            }
            try (ResultSet rs = db.getSubjectsByCourse(course)) {
                boolean found = false;
                if (rs != null)
                    while (rs.next()) {
                        if (rs.getString("subject_code").equalsIgnoreCase(code)) {
                            nameField.setText(rs.getString("subject_name"));
                            typeCombo.setSelectedItem(rs.getString("subject_type"));
                            found = true;
                            break;
                        }
                    }
                feedback(feedback, found,
                        found ? "Loaded — edit then Update or Delete." : "Not found for that course.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        addBtn.addActionListener(e -> {
            String code = codeField.getText().trim(), name = nameField.getText().trim();
            String course = (String) courseCombo.getSelectedItem();
            if (code.isEmpty() || name.isEmpty() || course == null) {
                feedback(feedback, false, "Code, name, and course required.");
                return;
            }
            boolean ok = db.addSubject(code, name, (String) typeCombo.getSelectedItem(), course);
            feedback(feedback, ok, ok ? "Subject added." : "Failed — duplicate (code + course)?");
            if (ok) {
                codeField.setText("");
                nameField.setText("");
                refresh.run();
            }
        });

        updateBtn.addActionListener(e -> {
            String course = (String) courseCombo.getSelectedItem();
            if (course == null) {
                feedback(feedback, false, "No course selected.");
                return;
            }
            boolean ok = db.updateSubject(codeField.getText().trim(), course, nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem());
            feedback(feedback, ok, ok ? "Updated." : "Failed.");
            if (ok)
                refresh.run();
        });

        delBtn.addActionListener(e -> {
            String code = codeField.getText().trim(), course = (String) courseCombo.getSelectedItem();
            if (code.isEmpty() || course == null) {
                feedback(feedback, false, "Enter code and select course.");
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Delete \"" + code + "\" from \"" + course + "\"?",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;
            boolean ok = db.deleteSubject(code, course);
            feedback(feedback, ok, ok ? "Deleted." : "Not found.");
            if (ok) {
                codeField.setText("");
                nameField.setText("");
                refresh.run();
            }
        });

        clearBtn.addActionListener(e -> {
            codeField.setText("");
            nameField.setText("");
            feedback.setText(" ");
        });

        p.add(row("Subject Code:", codeField));
        p.add(row("Subject Name:", nameField));
        p.add(row("Subject Type:", typeCombo));
        p.add(row("Course:", courseCombo));
        p.add(Box.createVerticalStrut(s(10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
        btnRow.setOpaque(false);
        btnRow.add(addBtn);
        btnRow.add(findBtn);
        btnRow.add(updateBtn);
        btnRow.add(delBtn);
        btnRow.add(clearBtn);
        p.add(btnRow);

        return new Object[] { p, refresh };
    }

    // ---- SECTION ----
    private Object[] buildSectionUnified(DefaultTableModel tableModel, JLabel feedback) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        java.util.Map<String, String> courseCodeMap = new java.util.LinkedHashMap<>();
        JComboBox<String> courseCombo = mkCombo(new String[0]);
        JComboBox<Integer> yearCombo = mkCombo(new Integer[] { 1, 2, 3, 4 });
        JComboBox<String> semCombo = mkCombo(new String[] { "A", "B", "C" });
        JTextField numField = mkField(6), idField = mkField(8);

        Runnable refreshCourses = () -> {
            String sel = (String) courseCombo.getSelectedItem();
            courseCodeMap.clear();
            courseCombo.removeAllItems();
            try (ResultSet rs = db.getCourses()) {
                if (rs != null)
                    while (rs.next()) {
                        String c = rs.getString("course");
                        courseCodeMap.put(c, rs.getString("course_code"));
                        courseCombo.addItem(c);
                    }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if (sel != null)
                courseCombo.setSelectedItem(sel);
        };
        refreshCourses.run();

        JButton addBtn = mkPrimaryBtn("Add");
        JButton findBtn = mkBtn("Load by ID");
        JButton updateBtn = mkBtn("Update");
        JButton delBtn = mkDangerBtn("Delete");
        JButton clearBtn = mkBtn("Clear");

        Runnable refresh = () -> {
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            ResultSet rs = db.getSections();
            if (rs == null)
                return;
            try {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                if (tableModel.getColumnCount() == 0)
                    for (int i = 1; i <= cols; i++)
                        tableModel.addColumn(meta.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 1; i <= cols; i++)
                        row[i - 1] = rs.getObject(i);
                    tableModel.addRow(row);
                }
            } catch (SQLException ignored) {
            }
        };

        findBtn.addActionListener(e -> {
            String text = idField.getText().trim();
            if (text.isEmpty()) {
                feedback(feedback, false, "Enter a Section ID.");
                return;
            }
            try {
                int id = Integer.parseInt(text);
                try (ResultSet rs = db.getSections()) {
                    boolean found = false;
                    if (rs != null)
                        while (rs.next()) {
                            if (rs.getInt("section_id") == id) {
                                courseCombo.setSelectedItem(rs.getString("course"));
                                yearCombo.setSelectedItem(rs.getInt("year"));
                                semCombo.setSelectedItem(rs.getString("semester"));
                                numField.setText(String.valueOf(rs.getInt("section_number")));
                                found = true;
                                break;
                            }
                        }
                    feedback(feedback, found, found ? "Loaded — edit then Update or Delete." : "Not found.");
                }
            } catch (NumberFormatException ex) {
                feedback(feedback, false, "ID must be a number.");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        addBtn.addActionListener(e -> {
            String course = (String) courseCombo.getSelectedItem();
            String numText = numField.getText().trim();
            if (course == null || numText.isEmpty()) {
                feedback(feedback, false, "All fields required.");
                return;
            }
            try {
                int newId = db.addSection(course, courseCodeMap.getOrDefault(course, ""),
                        (Integer) yearCombo.getSelectedItem(), (String) semCombo.getSelectedItem(),
                        Integer.parseInt(numText));
                if (newId != -1) {
                    feedback(feedback, true, "Section added (ID " + newId + ").");
                    numField.setText("");
                    refresh.run();
                } else
                    feedback(feedback, false, "Failed — duplicate?");
            } catch (NumberFormatException ex) {
                feedback(feedback, false, "Section number must be a number.");
            }
        });

        updateBtn.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String course = (String) courseCombo.getSelectedItem();
                boolean ok = db.updateSection(id, course, courseCodeMap.getOrDefault(course, ""),
                        (Integer) yearCombo.getSelectedItem(), (String) semCombo.getSelectedItem(),
                        Integer.parseInt(numField.getText().trim()));
                feedback(feedback, ok, ok ? "Updated." : "Failed.");
                if (ok)
                    refresh.run();
            } catch (NumberFormatException ex) {
                feedback(feedback, false, "Invalid number.");
            }
        });

        delBtn.addActionListener(e -> {
            String text = idField.getText().trim();
            if (text.isEmpty()) {
                feedback(feedback, false, "Enter a Section ID.");
                return;
            }
            try {
                int id = Integer.parseInt(text);
                if (JOptionPane.showConfirmDialog(this, "Delete section " + id + "? Students lose their section.",
                        "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
                boolean ok = db.deleteSection(id);
                feedback(feedback, ok, ok ? "Deleted." : "Not found.");
                if (ok) {
                    idField.setText("");
                    numField.setText("");
                    refresh.run();
                }
            } catch (NumberFormatException ex) {
                feedback(feedback, false, "ID must be a number.");
            }
        });

        clearBtn.addActionListener(e -> {
            idField.setText("");
            numField.setText("");
            feedback.setText(" ");
        });

        p.add(centered(mkLabel("Name auto-generated: Course Year Semester-Number", 12, false)));
        p.add(Box.createVerticalStrut(s(8)));
        p.add(row("Section ID (to load/edit/delete):", idField));
        p.add(row("Course:", courseCombo));
        p.add(row("Year Level:", yearCombo));
        p.add(row("Semester:", semCombo));
        p.add(row("Section Number:", numField));
        p.add(Box.createVerticalStrut(s(10)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
        btnRow.setOpaque(false);
        btnRow.add(addBtn);
        btnRow.add(findBtn);
        btnRow.add(updateBtn);
        btnRow.add(delBtn);
        btnRow.add(clearBtn);
        p.add(btnRow);

        return new Object[] { p, refresh };
    }

    // =========================================================================
    // STANDALONE PANELS (Enrollment, SectionSubject, SubjectIdLookup,
    // StudentSelfEdit) — kept, tables added inline
    // =========================================================================

    private class EnrollmentPanel extends ScaledPanel {
        EnrollmentPanel(double scale, JPanel nav) {
            super(scale);
            setLayout(new BorderLayout(0, s(14)));
            setBackground(CONTENT_BG);
            setBorder(new EmptyBorder(s(32), s(36), s(28), s(36)));

            JLabel title = new JLabel("Enrollment");
            title.setForeground(TEXT);
            title.setFont(font(Font.BOLD, 22));

            JButton enrollTab = mkBtn("Enroll");
            JButton unenrollTab = mkDangerBtn("Unenroll");
            JPanel modeBar = new JPanel(new FlowLayout(FlowLayout.LEFT, s(8), 0));
            modeBar.setOpaque(false);
            modeBar.add(enrollTab);
            modeBar.add(unenrollTab);

            JPanel header = new JPanel(new BorderLayout(0, s(10)));
            header.setOpaque(false);
            header.add(title, BorderLayout.NORTH);
            header.add(modeBar, BorderLayout.CENTER);

            JLabel feedback = mkFeedback();
            JTextField studentIdField = mkField(16), ssIdField = mkField(12);

            // Live table showing enrolled subjects for the student
            DefaultTableModel tableModel = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            JScrollPane tableScroll = makeLiveTablePane(tableModel);
            tableScroll.setPreferredSize(new Dimension(0, s(200)));

            Runnable refreshTable = () -> {
                String sid = studentIdField.getText().trim();
                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);
                if (sid.isEmpty())
                    return;
                ResultSet rs = db.getEnrolledSubjects(sid);
                if (rs == null)
                    return;
                try {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    if (tableModel.getColumnCount() == 0)
                        for (int i = 1; i <= cols; i++)
                            tableModel.addColumn(meta.getColumnLabel(i));
                    while (rs.next()) {
                        Object[] row = new Object[cols];
                        for (int i = 1; i <= cols; i++)
                            row[i - 1] = rs.getObject(i);
                        tableModel.addRow(row);
                    }
                } catch (SQLException ignored) {
                }
            };

            // Auto-refresh table when student ID changes
            studentIdField.addActionListener(e -> refreshTable.run());

            JButton actionBtn = mkPrimaryBtn("Enroll");
            JButton viewBtn = mkBtn("View Enrolled");

            viewBtn.addActionListener(e -> refreshTable.run());

            boolean[] enrollMode = { true };

            Runnable setEnroll = () -> {
                enrollMode[0] = true;
                actionBtn.setText("Enroll");
                actionBtn.setBackground(RED);
                enrollTab.setBackground(RED);
                unenrollTab.setBackground(CONTROL_BG);
            };
            Runnable setUnenroll = () -> {
                enrollMode[0] = false;
                actionBtn.setText("Unenroll");
                actionBtn.setBackground(new Color(120, 30, 30));
                enrollTab.setBackground(CONTROL_BG);
                unenrollTab.setBackground(new Color(120, 30, 30));
            };
            setEnroll.run();

            enrollTab.addActionListener(e -> {
                feedback.setText(" ");
                setEnroll.run();
            });
            unenrollTab.addActionListener(e -> {
                feedback.setText(" ");
                setUnenroll.run();
            });

            actionBtn.addActionListener(e -> {
                String sid = studentIdField.getText().trim(), ssText = ssIdField.getText().trim();
                if (sid.isEmpty() || ssText.isEmpty()) {
                    feedback(feedback, false, "Both fields required.");
                    return;
                }
                try {
                    int ssId = Integer.parseInt(ssText);
                    if (enrollMode[0]) {
                        String result = db.enrollStudent(sid, ssId);
                        feedback(feedback, result.startsWith("Enrolled"), result);
                        if (result.startsWith("Enrolled")) {
                            ssIdField.setText("");
                            refreshTable.run();
                        }
                    } else {
                        if (JOptionPane.showConfirmDialog(this, "Unenroll " + sid + " from SS ID " + ssId + "?",
                                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                            return;
                        boolean ok = db.unenrollStudent(sid, ssId);
                        feedback(feedback, ok, ok ? "Unenrolled." : "Not found.");
                        if (ok) {
                            ssIdField.setText("");
                            refreshTable.run();
                        }
                    }
                } catch (NumberFormatException ex) {
                    feedback(feedback, false, "Section Subject ID must be a number.");
                }
            });

            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.add(row("Student ID:", studentIdField));
            form.add(row("Section Subject ID:", ssIdField));
            form.add(Box.createVerticalStrut(s(10)));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
            btnRow.setOpaque(false);
            btnRow.add(actionBtn);
            btnRow.add(viewBtn);
            form.add(btnRow);

            // Table section
            JPanel tableHeader = new JPanel(new BorderLayout());
            tableHeader.setOpaque(false);
            tableHeader.add(mkLabel("Student's Enrolled Subjects", 13, true), BorderLayout.WEST);

            JPanel tableSection = new JPanel(new BorderLayout(0, s(6)));
            tableSection.setOpaque(false);
            tableSection.add(tableHeader, BorderLayout.NORTH);
            tableSection.add(tableScroll, BorderLayout.CENTER);

            JPanel formCard = new JPanel(new BorderLayout(0, s(10)));
            formCard.setBackground(CARD_BG);
            formCard.setBorder(
                    new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(s(20), s(24), s(20), s(24))));
            formCard.add(form, BorderLayout.CENTER);
            JPanel fbBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            fbBar.setOpaque(false);
            fbBar.add(feedback);
            formCard.add(fbBar, BorderLayout.SOUTH);

            JButton backBtn = mkBtn("← Back");
            backBtn.addActionListener(e -> showCard(nav, "choose"));
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            footer.setOpaque(false);
            footer.add(backBtn);

            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);
            headerRow.add(title, BorderLayout.WEST);
            headerRow.add(footer, BorderLayout.EAST);
            headerRow.add(modeBar, BorderLayout.SOUTH);

            JPanel center = new JPanel(new BorderLayout(0, s(14)));
            center.setOpaque(false);
            center.add(formCard, BorderLayout.NORTH);
            center.add(tableSection, BorderLayout.CENTER);

            add(headerRow, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
        }
    }

    private class SectionSubjectPanel extends ScaledPanel {
        SectionSubjectPanel(double scale, JPanel nav) {
            super(scale);
            setLayout(new BorderLayout(0, s(14)));
            setBackground(CONTENT_BG);
            setBorder(new EmptyBorder(s(24), s(36), s(24), s(36)));

            JLabel title = new JLabel("Section Subject");
            title.setForeground(TEXT);
            title.setFont(font(Font.BOLD, 22));

            JLabel feedback = mkFeedback();

            // ---- Live table ----
            DefaultTableModel tableModel = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            JScrollPane tableScroll = makeLiveTablePane(tableModel);
            tableScroll.setPreferredSize(new Dimension(0, s(200)));

            Runnable refresh = () -> {
                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);
                ResultSet rs = db.getSectionSubjects();
                if (rs == null)
                    return;
                try {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    if (tableModel.getColumnCount() == 0)
                        for (int i = 1; i <= cols; i++)
                            tableModel.addColumn(meta.getColumnLabel(i));
                    while (rs.next()) {
                        Object[] row = new Object[cols];
                        for (int i = 1; i <= cols; i++)
                            row[i - 1] = rs.getObject(i);
                        tableModel.addRow(row);
                    }
                } catch (SQLException ignored) {
                }
            };

            // ---- Unified form ----
            JPanel form = buildSecSubjUnified(tableModel, feedback, refresh);

            JPanel formCard = new JPanel(new BorderLayout(0, s(8)));
            formCard.setBackground(CARD_BG);
            formCard.setBorder(
                    new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(s(20), s(24), s(20), s(24))));
            formCard.add(form, BorderLayout.CENTER);
            JPanel fbBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            fbBar.setOpaque(false);
            fbBar.add(feedback);
            formCard.add(fbBar, BorderLayout.SOUTH);

            JLabel refreshIcon = mkLabel("↻ Refresh", 12, false);
            refreshIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            refreshIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    refresh.run();
                }
            });

            JPanel tableHeader = new JPanel(new BorderLayout());
            tableHeader.setOpaque(false);
            tableHeader.add(mkLabel("All Section Subjects", 13, true), BorderLayout.WEST);
            tableHeader.add(refreshIcon, BorderLayout.EAST);

            JPanel tableSection = new JPanel(new BorderLayout(0, s(6)));
            tableSection.setOpaque(false);
            tableSection.add(tableHeader, BorderLayout.NORTH);
            tableSection.add(tableScroll, BorderLayout.CENTER);

            JButton backBtn = mkBtn("← Back");
            backBtn.addActionListener(e -> showCard(nav, "choose"));
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            footer.setOpaque(false);
            footer.add(backBtn);

            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);
            headerRow.add(title, BorderLayout.WEST);
            headerRow.add(footer, BorderLayout.EAST);

            JPanel center = new JPanel(new BorderLayout(0, s(14)));
            center.setOpaque(false);
            center.add(formCard, BorderLayout.NORTH);
            center.add(tableSection, BorderLayout.CENTER);

            add(headerRow, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);

            refresh.run();
        }

        private JPanel buildSecSubjUnified(DefaultTableModel tableModel, JLabel feedback, Runnable refresh) {
            JPanel p = new JPanel();
            p.setOpaque(false);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

            // SS ID for load/edit/delete
            JTextField ssIdField = mkField(8);

            // Section picker
            Object[] sp = buildSectionPicker();
            @SuppressWarnings("unchecked")
            JComboBox<String> secCF = (JComboBox<String>) sp[0];
            @SuppressWarnings("unchecked")
            JComboBox<SectionItem> secC = (JComboBox<SectionItem>) sp[1];

            // Subject picker
            Object[] subP = buildSubjectPickerWithCourseFilter();
            @SuppressWarnings("unchecked")
            JComboBox<String> subCF = (JComboBox<String>) subP[0];
            @SuppressWarnings("unchecked")
            JComboBox<SubjectItem> subC = (JComboBox<SubjectItem>) subP[1];

            JComboBox<TeacherItem> tchC = buildTeacherCombo();

            JButton assignBtn = mkPrimaryBtn("Assign");
            JButton findBtn = mkBtn("Load by SS ID");
            JButton updateBtn = mkBtn("Update");
            JButton removeBtn = mkDangerBtn("Remove");
            JButton clearBtn = mkBtn("Clear");

            findBtn.addActionListener(e -> {
                String text = ssIdField.getText().trim();
                if (text.isEmpty()) {
                    feedback(feedback, false, "Enter a Section Subject ID.");
                    return;
                }
                try {
                    int id = Integer.parseInt(text);
                    try (ResultSet rs = db.getSectionSubjects()) {
                        boolean found = false;
                        if (rs != null)
                            while (rs.next()) {
                                if (rs.getInt("section_subject_id") == id) {
                                    int secId = rs.getInt("section_id");
                                    try (ResultSet allSecs = db.getSections()) {
                                        if (allSecs != null)
                                            while (allSecs.next()) {
                                                if (allSecs.getInt("section_id") == secId) {
                                                    secCF.setSelectedItem(allSecs.getString("course"));
                                                    for (int i = 0; i < secC.getItemCount(); i++)
                                                        if (secC.getItemAt(i).id == secId) {
                                                            secC.setSelectedIndex(i);
                                                            break;
                                                        }
                                                    break;
                                                }
                                            }
                                    } catch (SQLException ex2) {
                                        ex2.printStackTrace();
                                    }
                                    String subCourse = rs.getString("course"), subCode = rs.getString("subject_code");
                                    subCF.setSelectedItem(subCourse);
                                    for (int i = 0; i < subC.getItemCount(); i++) {
                                        SubjectItem si = subC.getItemAt(i);
                                        if (si.code.equals(subCode) && si.course.equals(subCourse)) {
                                            subC.setSelectedIndex(i);
                                            break;
                                        }
                                    }
                                    String tchId = rs.getString("teacher_id");
                                    for (int i = 0; i < tchC.getItemCount(); i++)
                                        if (tchC.getItemAt(i).id.equals(tchId)) {
                                            tchC.setSelectedIndex(i);
                                            break;
                                        }
                                    found = true;
                                    break;
                                }
                            }
                        feedback(feedback, found, found ? "Loaded — edit then Update or Remove." : "Not found.");
                    }
                } catch (NumberFormatException ex) {
                    feedback(feedback, false, "ID must be a number.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            assignBtn.addActionListener(e -> {
                SectionItem sec = (SectionItem) secC.getSelectedItem();
                SubjectItem sub = (SubjectItem) subC.getSelectedItem();
                TeacherItem tch = (TeacherItem) tchC.getSelectedItem();
                if (sec == null || sec.id == -1 || sub == null || tch == null) {
                    feedback(feedback, false, "All fields required.");
                    return;
                }
                int newId = db.assignSubjectToSection(sec.id, sub.code, sub.course, tch.id);
                if (newId != -1) {
                    feedback(feedback, true, "Assigned! SS ID = " + newId);
                    refresh.run();
                } else
                    feedback(feedback, false, "Failed — duplicate or DB error.");
            });

            updateBtn.addActionListener(e -> {
                try {
                    int id = Integer.parseInt(ssIdField.getText().trim());
                    SectionItem sec = (SectionItem) secC.getSelectedItem();
                    SubjectItem sub = (SubjectItem) subC.getSelectedItem();
                    TeacherItem tch = (TeacherItem) tchC.getSelectedItem();
                    if (sec == null || sec.id == -1 || sub == null || tch == null) {
                        feedback(feedback, false, "All fields required.");
                        return;
                    }
                    boolean ok = db.updateSectionSubject(id, sec.id, sub.code, sub.course, tch.id);
                    feedback(feedback, ok, ok ? "Updated." : "Failed.");
                    if (ok)
                        refresh.run();
                } catch (NumberFormatException ex) {
                    feedback(feedback, false, "Invalid ID.");
                }
            });

            removeBtn.addActionListener(e -> {
                String text = ssIdField.getText().trim();
                if (text.isEmpty()) {
                    feedback(feedback, false, "Enter a Section Subject ID.");
                    return;
                }
                try {
                    int id = Integer.parseInt(text);
                    if (JOptionPane.showConfirmDialog(null, "Remove SS assignment ID " + id + "?",
                            "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                        return;
                    boolean ok = db.removeSectionSubject(id);
                    feedback(feedback, ok, ok ? "Removed." : "Not found.");
                    if (ok) {
                        ssIdField.setText("");
                        refresh.run();
                    }
                } catch (NumberFormatException ex) {
                    feedback(feedback, false, "ID must be a number.");
                }
            });

            clearBtn.addActionListener(e -> {
                ssIdField.setText("");
                feedback.setText(" ");
            });

            // Left: section + SS ID
            JPanel leftFields = new JPanel();
            leftFields.setOpaque(false);
            leftFields.setLayout(new BoxLayout(leftFields, BoxLayout.Y_AXIS));
            leftFields.add(row("Section Subject ID:", ssIdField));
            leftFields.add(row("Section Course:", secCF));
            leftFields.add(row("Section:", secC));

            // Right: subject + teacher
            JPanel rightFields = new JPanel();
            rightFields.setOpaque(false);
            rightFields.setLayout(new BoxLayout(rightFields, BoxLayout.Y_AXIS));
            rightFields.add(row("Subject Course:", subCF));
            rightFields.add(row("Subject:", subC));
            rightFields.add(row("Teacher:", tchC));

            JPanel fieldsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(20), 0));
            fieldsRow.setOpaque(false);
            fieldsRow.add(leftFields);
            fieldsRow.add(rightFields);
            p.add(fieldsRow);
            p.add(Box.createVerticalStrut(s(10)));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
            btnRow.setOpaque(false);
            btnRow.add(assignBtn);
            btnRow.add(findBtn);
            btnRow.add(updateBtn);
            btnRow.add(removeBtn);
            btnRow.add(clearBtn);
            p.add(btnRow);

            return p;
        }
    }

    private class StudentSelfEditPanel extends ScaledPanel {
        StudentSelfEditPanel(double scale, JPanel nav) {
            super(scale);
            setLayout(new BorderLayout());
            setBackground(CONTENT_BG);

            JLabel feedback = mkFeedback();
            // Student self-edit reuses the student unified builder but without the
            // add/delete buttons
            // We build a minimal version: just load + update fields
            JTextField sidField = mkField(16), firstField = mkField(20), lastField = mkField(20),
                    miField = mkField(4), emailField = mkField(26);
            JCheckBox irregCheck = styledCheck("Irregular Student");
            Object[] picker = buildSectionPicker();
            @SuppressWarnings("unchecked")
            JComboBox<String> cf = (JComboBox<String>) picker[0];
            @SuppressWarnings("unchecked")
            JComboBox<SectionItem> sc = (JComboBox<SectionItem>) picker[1];

            JButton findBtn = mkBtn("Load My Profile");
            JButton updateBtn = mkPrimaryBtn("Save Changes");

            findBtn.addActionListener(e -> {
                String id = sidField.getText().trim();
                if (id.isEmpty()) {
                    feedback(feedback, false, "Enter your Student ID.");
                    return;
                }
                try (ResultSet rs = db.getStudents()) {
                    boolean found = false;
                    if (rs != null)
                        while (rs.next()) {
                            if (rs.getString("student_id").equalsIgnoreCase(id)) {
                                firstField.setText(rs.getString("first_name"));
                                lastField.setText(rs.getString("last_name"));
                                String mi = rs.getString("middle_initial");
                                miField.setText(mi == null ? "" : mi);
                                emailField.setText(rs.getString("email"));
                                irregCheck.setSelected(rs.getBoolean("is_irregular"));
                                int secId = rs.getInt("section_id");
                                if (!rs.wasNull()) {
                                    cf.setSelectedItem(rs.getString("course"));
                                    for (int i = 0; i < sc.getItemCount(); i++)
                                        if (sc.getItemAt(i).id == secId) {
                                            sc.setSelectedIndex(i);
                                            break;
                                        }
                                } else {
                                    sc.setSelectedIndex(0);
                                }
                                found = true;
                                break;
                            }
                        }
                    feedback(feedback, found, found ? "Loaded — edit then Save Changes." : "Not found.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            updateBtn.addActionListener(e -> {
                SectionItem sel = (SectionItem) sc.getSelectedItem();
                String mi = miField.getText().trim();
                boolean ok = db.updateStudent(sidField.getText().trim(), null,
                        firstField.getText().trim(), lastField.getText().trim(),
                        mi.isEmpty() ? null : mi, emailField.getText().trim(),
                        (sel == null || sel.id == -1) ? null : sel.id, irregCheck.isSelected());
                feedback(feedback, ok, ok ? "Profile updated." : "Failed.");
            });

            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.add(row("Student ID:", sidField));
            form.add(Box.createVerticalStrut(s(6)));
            form.add(centered(findBtn));
            form.add(Box.createVerticalStrut(s(10)));
            form.add(row("First Name:", firstField));
            form.add(row("Last Name:", lastField));
            form.add(row("Middle Initial:", miField));
            form.add(row("Email:", emailField));
            form.add(row("Course:", cf));
            form.add(row("Section:", sc));
            form.add(row("", irregCheck));
            form.add(Box.createVerticalStrut(s(10)));
            form.add(centered(updateBtn));

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(CARD_BG);
            wrapper.setBorder(
                    new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(s(24), s(28), s(24), s(28))));
            wrapper.add(form, BorderLayout.CENTER);
            JPanel fbBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            fbBar.setOpaque(false);
            fbBar.add(feedback);
            wrapper.add(fbBar, BorderLayout.SOUTH);

            JLabel title = new JLabel("Edit My Profile");
            title.setForeground(TEXT);
            title.setFont(font(Font.BOLD, 22));

            JButton backBtn = mkBtn("← Back");
            backBtn.addActionListener(e -> showCard(nav, "choose"));
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            footer.setOpaque(false);
            footer.add(backBtn);

            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);
            headerRow.add(title, BorderLayout.WEST);
            headerRow.add(footer, BorderLayout.EAST);

            JPanel outer = new JPanel(new BorderLayout(0, s(14)));
            outer.setBackground(CONTENT_BG);
            outer.setBorder(new EmptyBorder(s(32), s(36), s(28), s(36)));
            outer.add(headerRow, BorderLayout.NORTH);
            outer.add(wrapper, BorderLayout.CENTER);

            add(outer, BorderLayout.CENTER);
        }
    }

    private class SubjectIdPanel extends ScaledPanel {
        SubjectIdPanel(double scale, JPanel nav) {
            super(scale);
            setLayout(new BorderLayout(0, s(14)));
            setBackground(CONTENT_BG);
            setBorder(new EmptyBorder(s(32), s(36), s(28), s(36)));

            JLabel title = new JLabel("Section Subject ID Lookup");
            title.setForeground(TEXT);
            title.setFont(font(Font.BOLD, 22));

            JTextField sectionIdField = mkField(8);
            JButton filterBtn = mkBtn("Filter by Section");
            JButton viewAllBtn = mkBtn("View All");
            JLabel feedback = mkFeedback();

            DefaultTableModel tableModel = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            JScrollPane tableScroll = makeLiveTablePane(tableModel);
            tableScroll.setPreferredSize(new Dimension(0, s(300)));

            Runnable loadAll = () -> {
                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);
                ResultSet rs = db.getSectionSubjects();
                if (rs == null)
                    return;
                try {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    if (tableModel.getColumnCount() == 0)
                        for (int i = 1; i <= cols; i++)
                            tableModel.addColumn(meta.getColumnLabel(i));
                    while (rs.next()) {
                        Object[] row = new Object[cols];
                        for (int i = 1; i <= cols; i++)
                            row[i - 1] = rs.getObject(i);
                        tableModel.addRow(row);
                    }
                } catch (SQLException ignored) {
                }
                feedback(feedback, true, "Showing all section subjects.");
            };

            filterBtn.addActionListener(e -> {
                String text = sectionIdField.getText().trim();
                if (text.isEmpty()) {
                    feedback(feedback, false, "Enter a Section ID.");
                    return;
                }
                try {
                    int sectionId = Integer.parseInt(text);
                    tableModel.setRowCount(0);
                    tableModel.setColumnCount(0);
                    ResultSet rs = db.getSubjectsBySection(sectionId);
                    if (rs != null) {
                        try {
                            ResultSetMetaData meta = rs.getMetaData();
                            int cols = meta.getColumnCount();
                            if (tableModel.getColumnCount() == 0)
                                for (int i = 1; i <= cols; i++)
                                    tableModel.addColumn(meta.getColumnLabel(i));
                            while (rs.next()) {
                                Object[] row = new Object[cols];
                                for (int i = 1; i <= cols; i++)
                                    row[i - 1] = rs.getObject(i);
                                tableModel.addRow(row);
                            }
                        } catch (SQLException ignored) {
                        }
                        feedback(feedback, true, "Showing section " + sectionId + ".");
                    } else
                        feedback(feedback, false, "No results.");
                } catch (NumberFormatException ex) {
                    feedback(feedback, false, "Section ID must be a number.");
                }
            });

            viewAllBtn.addActionListener(e -> loadAll.run());

            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.add(centered(mkLabel("Use section_subject_id shown here when enrolling students.", 13, false)));
            form.add(Box.createVerticalStrut(s(12)));
            form.add(row("Section ID:", sectionIdField));
            form.add(Box.createVerticalStrut(s(8)));
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, s(8), 0));
            btnRow.setOpaque(false);
            btnRow.add(filterBtn);
            btnRow.add(viewAllBtn);
            form.add(btnRow);
            form.add(Box.createVerticalStrut(s(4)));
            form.add(centered(feedback));

            JPanel formCard = new JPanel(new BorderLayout());
            formCard.setBackground(CARD_BG);
            formCard.setBorder(
                    new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(s(20), s(24), s(20), s(24))));
            formCard.add(form, BorderLayout.CENTER);

            JPanel tableSection = new JPanel(new BorderLayout(0, s(6)));
            tableSection.setOpaque(false);
            tableSection.add(mkLabel("Section Subjects", 13, true), BorderLayout.NORTH);
            tableSection.add(tableScroll, BorderLayout.CENTER);

            JButton backBtn = mkBtn("← Back");
            backBtn.addActionListener(e -> showCard(nav, "choose"));
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            footer.setOpaque(false);
            footer.add(backBtn);

            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);
            headerRow.add(title, BorderLayout.WEST);
            headerRow.add(footer, BorderLayout.EAST);

            JPanel center = new JPanel(new BorderLayout(0, s(14)));
            center.setOpaque(false);
            center.add(formCard, BorderLayout.NORTH);
            center.add(tableSection, BorderLayout.CENTER);

            add(headerRow, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);

            // Load all on open
            loadAll.run();
        }
    }

    // =========================================================================
    // CHOICE PANEL
    // =========================================================================

    private class ChoicePanel extends ScaledPanel {
        ChoicePanel(double scale, JPanel nav) {
            super(scale);
            setBackground(CONTENT_BG);
            setBorder(new EmptyBorder(s(32), s(36), s(28), s(36)));
            setLayout(new BorderLayout(0, s(20)));

            JLabel title = new JLabel("Database Editor");
            title.setForeground(TEXT);
            title.setFont(font(Font.BOLD, 32));
            add(title, BorderLayout.NORTH);

            String[][] entities;
            if (role.equals("admin")) {
                entities = new String[][] {
                        { "Course / Program", "course" },
                        { "Teacher", "teacher" },
                        { "Student", "student" },
                        { "Subject", "subject" },
                        { "Section", "section" },
                        { "Enrollment", "enrollment" },
                        { "Section Subject", "sectionSubject" },
                        { "ID Lookup", "subjectIdPanel" },
                };
            } else if (role.equals("teacher")) {
                entities = new String[][] { { "Student", "student" } };
            } else {
                entities = new String[][] { { "My Profile", "student" } };
            }

            JPanel grid = new JPanel(new GridLayout(2, 4, s(14), s(14)));
            grid.setOpaque(false);
            for (String[] ent : entities) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(CARD_BG);
                card.setBorder(new CompoundBorder(
                        new LineBorder(BORDER, 1),
                        new EmptyBorder(s(16), s(18), s(16), s(18))));
                JLabel lbl = new JLabel(ent[0], SwingConstants.CENTER);
                lbl.setForeground(TEXT);
                lbl.setFont(font(Font.BOLD, 15));
                JButton btn = mkPrimaryBtn("Open");
                btn.addActionListener(e -> showCard(nav, ent[1]));
                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                btnPanel.setOpaque(false);
                btnPanel.add(btn);
                card.add(lbl, BorderLayout.CENTER);
                card.add(btnPanel, BorderLayout.SOUTH);
                grid.add(card);
            }
            add(grid, BorderLayout.CENTER);
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private JCheckBox styledCheck(String label) {
        JCheckBox cb = new JCheckBox(label);
        cb.setForeground(MUTED);
        cb.setBackground(CARD_BG);
        cb.setFont(font(Font.PLAIN, 13));
        return cb;
    }
}