package com.seatendance;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Permissions extends ScaledPanel {

    private final DatabaseHandler db;
    private final MainSeatendanceApp app;
    private JComboBox<ClassOptionItem> sectionCombo;
    private JPanel studentsPanel;

    public Permissions(double scale, MainSeatendanceApp app, DatabaseHandler db) {
        super(scale);
        this.app = app;
        this.db = db;

        setOpaque(true);
        setBackground(StartSessionPanel.BG);
        setLayout(new BorderLayout(0, s(16)));
        setBorder(new EmptyBorder(s(24), s(28), s(24), s(28)));

        add(buildHeader(), BorderLayout.NORTH);

        studentsPanel = new JPanel();
        studentsPanel.setOpaque(false);
        studentsPanel.setLayout(new BoxLayout(studentsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(studentsPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        add(scroll, BorderLayout.CENTER);
    }

    // Builds the title, subtitle, and class selector row.
    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Permissions");
        title.setForeground(StartSessionPanel.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, s(34)));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Grant students a login account for the app.");
        subtitle.setForeground(StartSessionPanel.MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, s(16)));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(s(4)));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(s(18)));
        header.add(buildSectionSelector());
        return header;
    }

    // Builds the class dropdown and load button.
    private JPanel buildSectionSelector() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, s(12), 0));
        row.setOpaque(false);

        JLabel label = new JLabel("Class:");
        label.setForeground(StartSessionPanel.MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, s(14)));

        List<ClassOptionItem> options = loadAllClassOptions();
        sectionCombo = new JComboBox<>(options.toArray(new ClassOptionItem[0]));
        sectionCombo.setFont(new Font("Segoe UI", Font.PLAIN, s(14)));
        sectionCombo.setPreferredSize(new Dimension(s(340), s(36)));
        sectionCombo.setForeground(StartSessionPanel.TEXT);
        sectionCombo.setBackground(StartSessionPanel.CONTROL_BG);
        sectionCombo.setFocusable(false);

        JButton loadButton = new JButton("Load Students");
        loadButton.setFont(new Font("Segoe UI", Font.BOLD, s(14)));
        loadButton.setPreferredSize(new Dimension(s(150), s(36)));
        loadButton.setFocusPainted(false);
        loadButton.addActionListener(e -> loadStudentsForSelectedClass());

        row.add(label);
        row.add(sectionCombo);
        row.add(loadButton);
        return row;
    }

    // Pulls every class offering from SectionSubjects.
    private List<ClassOptionItem> loadAllClassOptions() {
        List<ClassOptionItem> options = new ArrayList<>();
        for (AttendanceReport.ClassOption opt : db.getClassOptions()) {
            options.add(new ClassOptionItem(opt));
        }
        return options;
    }

    // Loads and displays student rows for the chosen class.
    private void loadStudentsForSelectedClass() {
        studentsPanel.removeAll();

        ClassOptionItem selected = (ClassOptionItem) sectionCombo.getSelectedItem();
        if (selected == null) {
            showMessage("No class selected.");
            studentsPanel.revalidate();
            studentsPanel.repaint();
            return;
        }

        try (ResultSet rs = db.getEnrolledStudents(selected.option.sectionSubjectId)) {
            if (rs == null) {
                showMessage("No students found.");
                studentsPanel.revalidate();
                studentsPanel.repaint();
                return;
            }
            boolean any = false;
            while (rs.next()) {
                any = true;
                String studentId = rs.getString("student_id");
                String first     = rs.getString("first_name");
                String last      = rs.getString("last_name");
                String email     = rs.getString("email");
                studentsPanel.add(buildStudentRow(studentId, first, last, email));
                studentsPanel.add(Box.createVerticalStrut(s(6)));
            }
            if (!any) showMessage("No students enrolled in this class.");
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error loading students.");
        }

        studentsPanel.revalidate();
        studentsPanel.repaint();
    }

    // Builds one student row with their info and a Grant Access button.
    private JPanel buildStudentRow(String studentId, String first, String last, String email) {
        JPanel row = new JPanel(new BorderLayout(s(12), 0));
        row.setOpaque(true);
        row.setBackground(StartSessionPanel.PANEL_BG);
        row.setBorder(new EmptyBorder(s(10), s(14), s(10), s(14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, s(56)));

        JPanel info = new JPanel(new GridLayout(2, 1, 0, s(2)));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(last + ", " + first + "  ·  " + studentId);
        nameLabel.setForeground(StartSessionPanel.TEXT);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, s(14)));

        JLabel emailLabel = new JLabel(email);
        emailLabel.setForeground(StartSessionPanel.MUTED);
        emailLabel.setFont(new Font("Segoe UI", Font.PLAIN, s(12)));

        info.add(nameLabel);
        info.add(emailLabel);

        // Check if this student already has a user account.
        boolean hasAccount = db.getUserByEmail(email) != null && hasEmailRow(email);
        JButton grantButton = new JButton(hasAccount ? "Has Account" : "Grant Access");
        grantButton.setFont(new Font("Segoe UI", Font.BOLD, s(13)));
        grantButton.setPreferredSize(new Dimension(s(140), s(34)));
        grantButton.setFocusPainted(false);
        grantButton.setEnabled(!hasAccount);
        if (!hasAccount) {
            grantButton.addActionListener(e -> promptGrantAccess(studentId, first, last, email, grantButton));
        }

        row.add(info, BorderLayout.CENTER);
        row.add(grantButton, BorderLayout.EAST);
        return row;
    }

    // Returns true if the email already exists in the Users table.
    private boolean hasEmailRow(String email) {
        try (ResultSet rs = db.getUserByEmail(email)) {
            return rs != null && rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Shows a password dialog and creates the user account if confirmed.
    private void promptGrantAccess(String studentId, String first, String last,
                                   String email, JButton grantButton) {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, s(8)));
        panel.setPreferredSize(new Dimension(s(320), s(110)));

        JLabel info = new JLabel("Set a password for " + first + " " + last + ":");
        info.setFont(new Font("Segoe UI", Font.PLAIN, s(13)));

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, s(13)));

        JPasswordField confirmField = new JPasswordField();
        confirmField.setFont(new Font("Segoe UI", Font.PLAIN, s(13)));

        panel.add(info);
        panel.add(labelledField("Password:", passwordField));
        panel.add(labelledField("Confirm:", confirmField));

        int result = JOptionPane.showConfirmDialog(
                this, panel,
                "Grant Account Access",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String password = new String(passwordField.getPassword()).trim();
        String confirm  = new String(confirmField.getPassword()).trim();

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String hash = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
        int userId = db.addUser(email, hash, "student");

        if (userId < 0) {
            JOptionPane.showMessageDialog(this,
                    "Failed to create account. The email may already be registered.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Link the new user_id back to the Students row.
        db.linkStudentUserId(studentId, userId);

        grantButton.setText("Has Account");
        grantButton.setEnabled(false);
        JOptionPane.showMessageDialog(this,
                "Account created for " + first + " " + last + ".\nThey can now log in with: " + email,
                "Account Created", JOptionPane.INFORMATION_MESSAGE);
    }

    // Wraps a label and field together in a small panel.
    private JPanel labelledField(String labelText, JComponent field) {
        JPanel wrapper = new JPanel(new BorderLayout(s(6), 0));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, s(12)));
        label.setPreferredSize(new Dimension(s(70), 0));
        wrapper.add(label, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
    }

    // Shows a single status/message label inside the students panel.
    private void showMessage(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(StartSessionPanel.MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, s(14)));
        studentsPanel.add(label);
    }

    // Wraps ClassOption so the combo box shows a readable label.
    private static final class ClassOptionItem {
        final AttendanceReport.ClassOption option;

        ClassOptionItem(AttendanceReport.ClassOption option) {
            this.option = option;
        }

        @Override
        public String toString() {
            return option.sectionName + "  —  " + option.subjectCode + "  (" + option.teacherName + ")";
        }
    }
}