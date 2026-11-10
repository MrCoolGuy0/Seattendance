package com.seatendance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;

/**
 * DatabaseHandler is the single database access class for the app.
 *
 * The project uses SQLite through JDBC:
 * - SQLite stores the whole database in one file, here named seatendance.db.
 * - JDBC is Java's standard database API.
 * - Connection means an open link to the database file.
 * - Statement runs plain SQL.
 * - PreparedStatement runs SQL with ? placeholders and safely fills values.
 * - ResultSet is the table-like result returned by SELECT queries.
 *
 * The panels/screens should call these methods instead of writing SQL directly.
 * That keeps the database rules in one place and makes the UI code simpler.
 *
 * Main schema idea:
 * - Users stores login data: email, password hash, and role.
 * - Admins, Teachers, and Students store profile data for each role.
 * - Courses contain course names/codes.
 * - Sections are course-year-semester-section groups.
 * - Subjects are subjects offered per course.
 * - SectionSubjects assigns one subject and teacher to one section.
 * - StudentEnrollments links students to the SectionSubjects they attend.
 * - AttendanceSessions tracks whether a class date is draft or concluded.
 * - Attendance stores each student's attendance status for a class date.
 */
public class DatabaseHandler {

    // Absolute path to the SQLite database file. The helper below finds the
    // Maven module folder first, so this works whether the app starts from the
    // repo root or from the seatendance module folder.
    private static final Path DB_PATH = resolveDatabasePath();

    // SQLite JDBC URL. Forward slashes are used because JDBC URLs are happier
    // with them, even on Windows paths.
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH.toString().replace('\\', '/');

    // -----------------------------------------------------------------------
    // Connection helper
    // -----------------------------------------------------------------------

    /**
     * Opens a fresh database connection.
     *
     * SQLite does not enforce foreign keys unless PRAGMA foreign_keys = ON is
     * executed for each connection, so every connection enables it here.
     */
    private Connection connect() throws SQLException {
        ensureDatabaseDirectory();
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    // -----------------------------------------------------------------------
    // Database & table setup
    // -----------------------------------------------------------------------

    /**
     * Creates/opens the SQLite database file.
     *
     * SQLite creates the file automatically when DriverManager opens the URL.
     * This method is mostly a startup sanity check and log message.
     */
    public void createDatabase() {
        try (Connection conn = connect()) {
            System.out.println("SQLite database ready: " + DB_PATH);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates every table used by the app.
     *
     * CREATE TABLE IF NOT EXISTS is safe to run many times. It creates missing
     * tables but leaves existing tables/data alone. The order matters because
     * foreign-key child tables must reference parent tables that already exist.
     */
    public void createTables() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            // Order matters because of foreign keys.
            stmt.executeUpdate(CREATE_USERS);
            stmt.executeUpdate(CREATE_ADMINS);
            stmt.executeUpdate(CREATE_COURSES);
            stmt.executeUpdate(CREATE_TEACHERS);
            stmt.executeUpdate(CREATE_SECTIONS);
            stmt.executeUpdate(CREATE_STUDENTS);
            stmt.executeUpdate(CREATE_SUBJECTS);
            stmt.executeUpdate(CREATE_SECTION_SUBJECTS);
            stmt.executeUpdate(CREATE_STUDENT_ENROLLMENTS);
            stmt.executeUpdate(CREATE_ATTENDANCE_SESSIONS);
            stmt.executeUpdate(CREATE_ATTENDANCE);
            stmt.executeUpdate(CREATE_SEAT_ASSIGNMENTS);
            stmt.executeUpdate(CREATE_SECTIONS_NAME_INDEX);

            // Ensure seat_number column exists for existing databases
            try {
                stmt.executeUpdate("ALTER TABLE Attendance ADD COLUMN seat_number INTEGER");
            } catch (SQLException ignored) {
                // Column probably already exists
            }
            try {
                stmt.executeUpdate("ALTER TABLE StudentEnrollments ADD COLUMN seat_number INTEGER");
            } catch (SQLException ignored) {
                // Column probably already exists
            }
            try {
                stmt.executeUpdate("ALTER TABLE AttendanceSessions ADD COLUMN start_time TEXT");
            } catch (SQLException ignored) {
            }
            try {
                stmt.executeUpdate("ALTER TABLE AttendanceSessions ADD COLUMN end_time TEXT");
            } catch (SQLException ignored) {
            }
            try {
                stmt.executeUpdate("ALTER TABLE AttendanceSessions ADD COLUMN classroom_type TEXT");
            } catch (SQLException ignored) {
            }

            System.out.println("All tables ready.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // USERS
    //
    // Users is the login table. It only knows email, password hash, role, and
    // creation time. The actual profile data is in Admins/Teachers/Students.
    // -----------------------------------------------------------------------

    /**
     * Inserts a login account and returns the generated user_id.
     *
     * Statement.RETURN_GENERATED_KEYS asks SQLite/JDBC for the AUTOINCREMENT id
     * created by the insert. The method returns -1 if the insert fails.
     */
    public int addUser(String email, String passwordHash, String role) {
        String sql = "INSERT INTO Users (email, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, role);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Returns all login accounts for admin/database-management screens.
     *
     * Important: this method returns a ResultSet directly, so the caller reads
     * rows from it. The connection stays open behind that ResultSet.
     */
    public ResultSet getUsers() {
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery("SELECT user_id, email, role, created_at FROM Users ORDER BY created_at DESC");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Finds one login account by email.
     *
     * Used by login/auth code. The ? placeholder prevents SQL injection and
     * avoids manual quote escaping.
     */
    public ResultSet getUserByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email = ?";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Looks up the account role: admin, teacher, student, or null if missing.
     */
    public String getUserRole(String email) {
        String sql = "SELECT role FROM Users WHERE email = ?";
        String role = null;

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    role = rs.getString("role");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return role;
    }

    /**
     * Gets the display first name for a user.
     *
     * Because profile data is split by role, the method first reads Users.role,
     * then queries the matching profile table.
     */
    public String getUserName(String email) {
        String role = getUserRole(email);

        if (role == null) {
            return null;
        }

        if (role.equals("admin")) {
            String sql = "SELECT first_name FROM Admins WHERE email = ?";
            try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("first_name");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals("teacher")) {
            String sql = "SELECT first_name FROM Teachers WHERE email = ?";
            try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("first_name");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (role.equals("student")) {
            String sql = "SELECT first_name FROM Students WHERE email = ?";
            try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("first_name");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Gets the stored password hash for login verification.
     *
     * This should be a BCrypt hash, not the plain password.
     */
    public String getUserPassword(String email) {
        String sql = "SELECT password_hash FROM Users WHERE email = ?";
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Deletes a login account by numeric user_id.
     *
     * If another table still references this user through a foreign key, SQLite
     * can reject the delete. That is useful because it prevents orphan profiles.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE user_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Seeds a default admin account if it does not already exist.
     *
     * Initial administrator credentials are supplied through environment variables.
     *
     * This creates both the Users login row and the Admins profile row. It is
     * idempotent, meaning running it again should not create duplicates.
     */
    public void seedDefaultAdmin() {
        String defaultEmail = System.getenv("SEATTENDANCE_ADMIN_EMAIL");
        String defaultPassword = System.getenv("SEATTENDANCE_ADMIN_PASSWORD");
        if (defaultEmail == null || defaultEmail.isBlank()
                || defaultPassword == null || defaultPassword.isBlank()) {
            System.err.println("Initial admin setup skipped: set SEATTENDANCE_ADMIN_EMAIL and SEATTENDANCE_ADMIN_PASSWORD.");
            return;
        }
        String userSql = "SELECT user_id, role FROM Users WHERE email = ?";
        String adminSql = "SELECT COUNT(*) FROM Admins WHERE email = ?";
        Integer userId = null;
        String role = null;
        boolean adminProfileExists = false;
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(userSql)) {
            ps.setString(1, defaultEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                    role = rs.getString("role");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (userId != null) {
            try (Connection conn = connect();
                    PreparedStatement ps = conn.prepareStatement(adminSql)) {
                ps.setString(1, defaultEmail);

                try (ResultSet rs = ps.executeQuery()) {
                    adminProfileExists = rs.next() && rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (userId == null) {
            String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
                    defaultPassword,
                    org.mindrot.jbcrypt.BCrypt.gensalt());

            userId = addUser(defaultEmail, hash, "admin");
            role = "admin";
            System.out.println("Initial administrator account created.");
        }

        if (userId != null && "admin".equalsIgnoreCase(role) && !adminProfileExists) {
            addAdmin(userId, "Admin", "Admin", null, defaultEmail);
        }
    }

    // -----------------------------------------------------------------------
    // ADMINS
    //
    // Admins is the profile table for users whose Users.role is admin.
    // -----------------------------------------------------------------------

    /**
     * Creates the admin profile row linked to an existing Users row.
     */
    public boolean addAdmin(int userId, String firstName, String lastName, String middleInitial, String email) {
        String sql = "INSERT INTO Admins (user_id, first_name, last_name, middle_initial, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, middleInitial);
            ps.setString(5, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // COURSES
    //
    // Courses are the degree/program names such as BSIT/BSCS/etc. Year level is
    // not stored here because year belongs to a specific section.
    // -----------------------------------------------------------------------

    /**
     * Adds a course/program.
     */
    public boolean addCourse(String course, String courseCode) {
        String sql = "INSERT INTO Courses (course, course_code) VALUES (?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course);
            ps.setString(2, courseCode);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the short code for an existing course.
     *
     * The course name is the primary key, so this method identifies the row by
     * course and changes only course_code.
     */
    public boolean updateCourse(String course, String newCourseCode) {
        String sql = "UPDATE Courses SET course_code = ? WHERE course = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newCourseCode);
            ps.setString(2, course);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a course/program by name.
     *
     * Foreign keys can block this if sections or subjects still reference it.
     */
    public boolean deleteCourse(String course) {
        String sql = "DELETE FROM Courses WHERE course = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns all courses for table/list UI screens.
     */
    public ResultSet getCourses() {
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery("SELECT * FROM Courses ORDER BY course");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns only course names as a String array, useful for combo boxes.
     */
    public String[] getCourseNames() {
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT course FROM Courses ORDER BY course");

            ArrayList<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("course"));
            }
            return list.toArray(new String[0]);

        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    /**
     * Returns only course codes as a String array, useful for UI dropdowns.
     */
    public String[] getCourseCodes() {
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT course_code FROM Courses ORDER BY course");

            ArrayList<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("course_code"));
            }
            return list.toArray(new String[0]);

        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    // -----------------------------------------------------------------------
    // TEACHERS
    //
    // Teachers stores profile rows for teacher users. teacher_id is the main
    // primary key; uid is the RFID/card UID and must be unique.
    // -----------------------------------------------------------------------

    /**
     * Adds a teacher profile linked to an existing Users account.
     */
    public boolean addTeacher(String uid, String teacherId, int userId, String firstName, String lastName,
            String middleInitial, String email) {
        String sql = "INSERT INTO Teachers (uid, teacher_id, user_id, first_name, last_name, middle_initial, email) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, teacherId);
            ps.setInt(3, userId);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.setString(6, middleInitial);
            ps.setString(7, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates teacher name fields using teacher_id as the row identifier.
     */
    public boolean updateTeacher(String teacherId, String firstName, String lastName, String middleInitial) {
        String sql = "UPDATE Teachers SET first_name = ?, last_name = ?, middle_initial = ? WHERE teacher_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, middleInitial);
            ps.setString(4, teacherId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a teacher by teacher_id.
     *
     * This can fail if that teacher is still assigned in SectionSubjects.
     */
    public boolean deleteTeacher(String teacherId) {
        String sql = "DELETE FROM Teachers WHERE teacher_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teacherId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns teacher rows together with the email from Users.
     *
     * JOIN combines Teachers and Users because teacher email/login data is tied
     * to the user_id foreign key.
     */
    public ResultSet getTeachers() {
        String sql = "SELECT t.uid, t.teacher_id, t.user_id, u.email, t.first_name, t.last_name, t.middle_initial " +
                "FROM Teachers t JOIN Users u ON t.user_id = u.user_id " +
                "ORDER BY t.last_name, t.first_name";
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // SECTIONS
    //
    // Sections represent class blocks under a course, like BSIT 2 YA-1. The
    // section_name column is generated by SQLite from course_code/year/semester/no.
    // -----------------------------------------------------------------------

    /**
     * Adds a section and returns the generated section_id.
     *
     * course_code is stored alongside course so the generated section_name
     * can use the short code without needing a JOIN.
     */
    public int addSection(String course, String courseCode, int year, String semester, int sectionNumber) {
        String sql = "INSERT INTO Sections (course, course_code, year, semester, section_number) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, course);
            ps.setString(2, courseCode);
            ps.setInt(3, year);
            ps.setString(4, semester);
            ps.setInt(5, sectionNumber);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Updates the parts of a section name.
     *
     * section_name itself is generated, so the code updates its source columns.
     * course_code is kept in sync so the generated name stays correct.
     */
    public boolean updateSection(int sectionId, String course, String courseCode, int year, String semester,
            int sectionNumber) {
        String sql = "UPDATE Sections SET course = ?, course_code = ?, year = ?, semester = ?, section_number = ? WHERE section_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, course);
            ps.setString(2, courseCode);
            ps.setInt(3, year);
            ps.setString(4, semester);
            ps.setInt(5, sectionNumber);
            ps.setInt(6, sectionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a section by id.
     *
     * Existing students or section-subject assignments can block deletion.
     */
    public boolean deleteSection(int sectionId) {
        String sql = "DELETE FROM Sections WHERE section_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns all sections ordered in a human-friendly course/year order.
     */
    public ResultSet getSections() {
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery("SELECT * FROM Sections ORDER BY course, year, semester, section_number");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns sections for one course/program.
     */
    public ResultSet getSectionsByCourse(String course) {
        String sql = "SELECT * FROM Sections WHERE course = ? ORDER BY year, semester, section_number";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, course);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // STUDENTS
    //
    // Students stores student profile rows. user_id and section_id are nullable
    // because a student may exist before an app login or fixed block section is
    // assigned. is_irregular is stored as 0/1 by SQLite.
    // -----------------------------------------------------------------------

    /**
     * Adds a student profile.
     *
     * setNullableInt is used for userId and sectionId because those database
     * columns allow NULL.
     */
    public boolean addStudent(String uid, String studentId, Integer userId, String firstName, String lastName,
            String middleInitial, String email, Integer sectionId, boolean isIrregular) {
        String sql = "INSERT INTO Students (uid, student_id, user_id, first_name, last_name, middle_initial, email, section_id, is_irregular) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, studentId);
            setNullableInt(ps, 3, userId);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.setString(6, middleInitial);
            ps.setString(7, email);
            setNullableInt(ps, 8, sectionId);
            ps.setBoolean(9, isIrregular);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates a student profile by student_id.
     */
    public boolean updateStudent(String studentId, Integer userId, String firstName, String lastName,
            String middleInitial, String email, Integer sectionId, boolean isIrregular) {
        String sql = "UPDATE Students SET user_id = ?, first_name = ?, last_name = ?, middle_initial = ?, " +
                "email = ?, section_id = ?, is_irregular = ? WHERE student_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableInt(ps, 1, userId);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, middleInitial);
            ps.setString(5, email);
            setNullableInt(ps, 6, sectionId);
            ps.setBoolean(7, isIrregular);
            ps.setString(8, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a student by student_id.
     *
     * This can fail if the student is still enrolled or has attendance rows.
     */
    public boolean deleteStudent(String studentId) {
        String sql = "DELETE FROM Students WHERE student_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns all students and includes section data when the student has a
     * section_id. LEFT JOIN keeps students with no section in the result.
     */
    public ResultSet getStudents() {
        String sql = "SELECT s.*, sec.section_name, sec.course, sec.year " +
                "FROM Students s LEFT JOIN Sections sec ON s.section_id = sec.section_id " +
                "ORDER BY s.last_name, s.first_name";
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns students assigned to one section.
     */
    public ResultSet getStudentsBySection(int sectionId) {
        String sql = "SELECT * FROM Students WHERE section_id = ? ORDER BY last_name, first_name";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sectionId);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns students whose section belongs to a specific course/program.
     */
    public ResultSet getStudentsByCourse(String course) {
        String sql = "SELECT s.*, sec.section_name " +
                "FROM Students s JOIN Sections sec ON s.section_id = sec.section_id " +
                "WHERE sec.course = ? " +
                "ORDER BY s.last_name, s.first_name";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, course);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // SUBJECTS
    //
    // composite PK (subject_code, course) means the pair must be unique.
    // Minor subjects can belong to
    // multiple courses by having one row per course.
    // -----------------------------------------------------------------------

    /**
     * Adds a subject for a course.
     */
    public boolean addSubject(String subjectCode, String subjectName, String subjectType, String course) {
        String sql = "INSERT INTO Subjects (subject_code, subject_name, subject_type, course) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subjectCode);
            ps.setString(2, subjectName);
            ps.setString(3, subjectType);
            ps.setString(4, course);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates a subject's name/type while keeping its subject_code and course.
     */
    public boolean updateSubject(String subjectCode, String course, String subjectName, String subjectType) {
        String sql = "UPDATE Subjects SET subject_name = ?, subject_type = ? WHERE subject_code = ? AND course = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subjectName);
            ps.setString(2, subjectType);
            ps.setString(3, subjectCode);
            ps.setString(4, course);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes one subject row for one course.
     */
    public boolean deleteSubject(String subjectCode, String course) {
        String sql = "DELETE FROM Subjects WHERE subject_code = ? AND course = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subjectCode);
            ps.setString(2, course);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns all subjects across all courses.
     */
    public ResultSet getSubjects() {
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery("SELECT * FROM Subjects ORDER BY course, subject_code");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns subjects for a single course/program.
     */
    public ResultSet getSubjectsByCourse(String course) {
        String sql = "SELECT * FROM Subjects WHERE course = ? ORDER BY subject_code";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, course);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // SECTION SUBJECTS
    //
    // course column added to enforce FK against Subjects(subject_code, course).
    // This table is the "class offering": one section taking one subject with
    // one assigned teacher.
    // -----------------------------------------------------------------------

    /**
     * Assigns a subject to a section and teacher.
     *
     * Returns the generated section_subject_id used by enrollment/attendance.
     */
    public int assignSubjectToSection(int sectionId, String subjectCode, String course, String teacherId) {
        String sql = "INSERT INTO SectionSubjects (section_id, subject_code, course, teacher_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sectionId);
            ps.setString(2, subjectCode);
            ps.setString(3, course);
            ps.setString(4, teacherId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Changes the section, subject, course, or teacher for one class offering.
     */
    public boolean updateSectionSubject(int sectionSubjectId, int sectionId, String subjectCode, String course,
            String teacherId) {
        String sql = "UPDATE SectionSubjects SET section_id = ?, subject_code = ?, course = ?, teacher_id = ? WHERE section_subject_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ps.setString(2, subjectCode);
            ps.setString(3, course);
            ps.setString(4, teacherId);
            ps.setInt(5, sectionSubjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes one class offering.
     *
     * This can fail if students/attendance still reference this offering.
     */
    public boolean removeSectionSubject(int sectionSubjectId) {
        String sql = "DELETE FROM SectionSubjects WHERE section_subject_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns every class offering with readable section, subject, and teacher
     * names. This is mostly for admin/editor tables.
     */
    public ResultSet getSectionSubjects() {
        String sql = "SELECT ss.section_subject_id, ss.section_id, sec.section_name, " +
                "ss.subject_code, ss.course, sub.subject_name, sub.subject_type, ss.teacher_id, " +
                "t.last_name || ', ' || t.first_name AS teacher_name " +
                "FROM SectionSubjects ss " +
                "JOIN Sections sec ON ss.section_id = sec.section_id " +
                "JOIN Subjects sub ON ss.subject_code = sub.subject_code AND ss.course = sub.course " +
                "JOIN Teachers t ON ss.teacher_id = t.teacher_id " +
                "ORDER BY sec.section_name, ss.subject_code";
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns all subjects assigned to a given section.
     */
    public ResultSet getSubjectsBySection(int sectionId) {
        String sql = "SELECT ss.section_subject_id, ss.subject_code, ss.course, sub.subject_name, sub.subject_type, " +
                "ss.teacher_id, t.last_name || ', ' || t.first_name AS teacher_name " +
                "FROM SectionSubjects ss " +
                "JOIN Subjects sub ON ss.subject_code = sub.subject_code AND ss.course = sub.course " +
                "JOIN Teachers t ON ss.teacher_id = t.teacher_id " +
                "WHERE ss.section_id = ? " +
                "ORDER BY ss.subject_code";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sectionId);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the class offerings handled by a specific teacher.
     */
    public ResultSet getSubjectsByTeacher(String teacherId) {
        String sql = "SELECT ss.section_subject_id, ss.course, sec.section_name, ss.subject_code, sub.subject_name, sub.subject_type "
                +
                "FROM SectionSubjects ss " +
                "JOIN Sections sec ON ss.section_id = sec.section_id " +
                "JOIN Subjects sub ON ss.subject_code = sub.subject_code AND ss.course = sub.course " +
                "WHERE ss.teacher_id = ? " +
                "ORDER BY sec.section_name, ss.subject_code";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, teacherId);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the email address of the teacher assigned to the given
     * section_subject_id, or null if not found.
     *
     * The query walks SectionSubjects -> Teachers -> Users to get the login
     * email for notification features.
     */
    public String getTeacherEmailBySectionSubject(int sectionSubjectId) {
        String sql = "SELECT u.email " +
                "FROM SectionSubjects ss " +
                "JOIN Teachers t ON ss.teacher_id = t.teacher_id " +
                "JOIN Users u ON t.user_id = u.user_id " +
                "WHERE ss.section_subject_id = ?";
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // STUDENT ENROLLMENTS
    //
    // StudentEnrollments is a junction table. It connects many students to many
    // section-subject offerings.
    // -----------------------------------------------------------------------

    /**
     * Enrolls a student in a class offering.
     * Returns a status message string instead of a boolean to provide clear UI
     * feedback.
     */
    public String enrollStudent(String studentId, int sectionSubjectId) {
        String sql = "INSERT INTO StudentEnrollments (student_id, section_subject_id) VALUES (?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, sectionSubjectId);
            ps.executeUpdate();
            return "Enrolled successfully.";
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null) {
                if (msg.contains("SQLITE_CONSTRAINT_PRIMARYKEY") || msg.contains("UNIQUE constraint failed")) {
                    return "Failed: Student is already enrolled in this class.";
                } else if (msg.contains("SQLITE_CONSTRAINT_FOREIGNKEY")
                        || msg.contains("FOREIGN KEY constraint failed")) {
                    return "Failed: Student ID or Section Subject ID does not exist.";
                }
            }
            e.printStackTrace();
            return "Failed: Database error.";
        }
    }

    /**
     * Removes a student's enrollment from a class offering.
     */
    public boolean unenrollStudent(String studentId, int sectionSubjectId) {
        String sql = "DELETE FROM StudentEnrollments WHERE student_id = ? AND section_subject_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, sectionSubjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns all class offerings a student is enrolled in.
     */
    public ResultSet getEnrolledSubjects(String studentId) {
        String sql = "SELECT se.section_subject_id, ss.subject_code, ss.course, sub.subject_name, sub.subject_type, " +
                "sec.section_name, ss.teacher_id, t.last_name || ', ' || t.first_name AS teacher_name " +
                "FROM StudentEnrollments se " +
                "JOIN SectionSubjects ss ON se.section_subject_id = ss.section_subject_id " +
                "JOIN Subjects sub ON ss.subject_code = sub.subject_code AND ss.course = sub.course " +
                "JOIN Sections sec ON ss.section_id = sec.section_id " +
                "JOIN Teachers t ON ss.teacher_id = t.teacher_id " +
                "WHERE se.student_id = ? " +
                "ORDER BY sec.section_name, ss.subject_code";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns students enrolled in one class offering.
     */
    public ResultSet getEnrolledStudents(int sectionSubjectId) {
        String sql = "SELECT se.student_id, s.first_name, s.last_name, s.middle_initial, s.email, se.seat_number " +
                "FROM StudentEnrollments se " +
                "JOIN Students s ON se.student_id = s.student_id " +
                "WHERE se.section_subject_id = ? " +
                "ORDER BY s.last_name, s.first_name";
        try {
            Connection conn = connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sectionSubjectId);
            return ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // ATTENDANCE
    //
    // AttendanceSessions tracks the class date state. Attendance tracks each
    // student's mark on that date. Status values are limited by CHECK rules.
    // -----------------------------------------------------------------------

    /**
     * Inserts or updates one student's attendance mark for a date.
     *
     * ON CONFLICT is SQLite's upsert syntax. Because Attendance has UNIQUE
     * (student_id, section_subject_id, date), scanning/marking again updates
     * the existing row instead of creating duplicates.
     */
    public boolean markAttendance(String studentId, int sectionSubjectId, Date date, Time timeIn, Time timeOut,
            String status, int seatNumber) {
        String sql = "INSERT INTO Attendance (student_id, section_subject_id, date, time_in, time_out, status, seat_number) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(student_id, section_subject_id, date) DO UPDATE SET " +
                "time_in = excluded.time_in, time_out = excluded.time_out, status = excluded.status, " +
                "seat_number = COALESCE(excluded.seat_number, Attendance.seat_number)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, sectionSubjectId);
            ps.setDate(3, date);
            ps.setTime(4, timeIn);
            ps.setTime(5, timeOut);
            ps.setString(6, status);
            if (seatNumber > 0) {
                ps.setInt(7, seatNumber);
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // SEAT ASSIGNMENTS
    //
    // SeatAssignments stores per-classroom-type seat numbers. Each student
    // can have a different seat in Regular Classroom vs Computer Laboratory.
    // Seats are assigned randomly within a compact range (1..N) where N is
    // the total enrolled student count for the class.
    // -----------------------------------------------------------------------

    /**
     * Assigns a seat to a student for a specific classroom type.
     * Uses INSERT OR REPLACE so re-assigning updates the existing row.
     */
    public boolean assignSeat(String studentId, int sectionSubjectId, String classroomType, int seatNumber) {
        String sql = "INSERT OR REPLACE INTO SeatAssignments (student_id, section_subject_id, classroom_type, seat_number) "
                +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, sectionSubjectId);
            ps.setString(3, classroomType);
            ps.setInt(4, seatNumber);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the seat number for a student in a specific classroom type,
     * or -1 if the student has no seat assigned for that type yet.
     */
    public int getStudentSeat(String studentId, int sectionSubjectId, String classroomType) {
        String sql = "SELECT seat_number FROM SeatAssignments " +
                "WHERE student_id = ? AND section_subject_id = ? AND classroom_type = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, sectionSubjectId);
            ps.setString(3, classroomType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Clears all seat assignments for a class in a specific classroom type.
     */
    public boolean resetSeats(int sectionSubjectId, String classroomType) {
        String sql = "DELETE FROM SeatAssignments WHERE section_subject_id = ? AND classroom_type = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setString(2, classroomType);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes a single student's seat assignment for a specific classroom type.
     * Called when a student is changed to absent/excused/unmarked.
     */
    public boolean removeSeat(String studentId, int sectionSubjectId, String classroomType) {
        String sql = "DELETE FROM SeatAssignments WHERE student_id = ? AND section_subject_id = ? AND classroom_type = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, sectionSubjectId);
            ps.setString(3, classroomType);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if any seat assignments exist for a given section and classroom type.
     */
    public boolean hasSeatAssignments(int sectionSubjectId, String classroomType) {
        String sql = "SELECT COUNT(*) FROM SeatAssignments WHERE section_subject_id = ? AND classroom_type = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setString(2, classroomType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if any student has a non-null attendance status for a specific session
     * date.
     * Returns true if at least one student has been marked (Present, Late, Absent,
     * etc.)
     */
    public boolean hasAnyAttendance(int sectionSubjectId, Date date) {
        String sql = "SELECT COUNT(*) FROM Attendance WHERE section_subject_id = ? AND date = ? AND status IS NOT NULL";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Returns the total number of students enrolled in a class offering.
     */
    public int getEnrolledStudentCount(int sectionSubjectId) {
        String sql = "SELECT COUNT(*) FROM StudentEnrollments WHERE section_subject_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Picks a random available seat within range 1..N for a classroom type.
     * N is the total enrolled student count. Returns -1 if all seats taken.
     */
    public int getRandomAvailableSeat(int sectionSubjectId, String classroomType) {
        int total = getEnrolledStudentCount(sectionSubjectId);
        if (total <= 0)
            return 1;

        // Get all currently occupied seats for this classroom type
        String sql = "SELECT seat_number FROM SeatAssignments " +
                "WHERE section_subject_id = ? AND classroom_type = ?";
        java.util.Set<Integer> occupied = new java.util.HashSet<>();
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setString(2, classroomType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    occupied.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Build list of available seats in 1..total
        java.util.List<Integer> available = new java.util.ArrayList<>();
        for (int i = 1; i <= total; i++) {
            if (!occupied.contains(i))
                available.add(i);
        }

        if (available.isEmpty())
            return -1;
        java.util.Collections.shuffle(available);
        return available.get(0);
    }

    /**
     * Assigns random seats to all enrolled students who don't have one yet
     * for the given classroom type. Called when a session is concluded so
     * absent students also get a permanent seat.
     */
    public void assignRemainingSeats(int sectionSubjectId, String classroomType) {
        int total = getEnrolledStudentCount(sectionSubjectId);
        if (total <= 0)
            return;

        // Get all students who already have seats
        java.util.Set<Integer> occupied = new java.util.HashSet<>();
        java.util.Set<String> assigned = new java.util.HashSet<>();
        String sqlAssigned = "SELECT student_id, seat_number FROM SeatAssignments " +
                "WHERE section_subject_id = ? AND classroom_type = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sqlAssigned)) {
            ps.setInt(1, sectionSubjectId);
            ps.setString(2, classroomType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assigned.add(rs.getString(1));
                    occupied.add(rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Get all enrolled students
        java.util.List<String> unassigned = new java.util.ArrayList<>();
        String sqlAll = "SELECT student_id FROM StudentEnrollments WHERE section_subject_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sqlAll)) {
            ps.setInt(1, sectionSubjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sid = rs.getString(1);
                    if (!assigned.contains(sid))
                        unassigned.add(sid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if (unassigned.isEmpty())
            return;

        // Build available seats and shuffle
        java.util.List<Integer> available = new java.util.ArrayList<>();
        for (int i = 1; i <= total; i++) {
            if (!occupied.contains(i))
                available.add(i);
        }
        java.util.Collections.shuffle(available);
        java.util.Collections.shuffle(unassigned);

        // Assign one seat per unassigned student
        int count = Math.min(unassigned.size(), available.size());
        for (int i = 0; i < count; i++) {
            assignSeat(unassigned.get(i), sectionSubjectId, classroomType, available.get(i));
        }
    }

    /**
     * Kept for backward compatibility. Delegates to the old enrollment-based
     * seat column. New code should use assignSeat() instead.
     */
    @Deprecated
    public boolean assignPermanentSeat(String studentId, int sectionSubjectId, int seatNumber) {
        String sql = "UPDATE StudentEnrollments SET seat_number = ? WHERE student_id = ? AND section_subject_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatNumber);
            ps.setString(2, studentId);
            ps.setInt(3, sectionSubjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kept for backward compatibility. New code should use resetSeats() instead.
     */
    @Deprecated
    public boolean resetPermanentSeats(int sectionSubjectId) {
        String sql = "UPDATE StudentEnrollments SET seat_number = NULL WHERE section_subject_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kept for backward compatibility. New code should use getRandomAvailableSeat()
     * instead.
     */
    @Deprecated
    public int getNextAvailableSeat(int sectionSubjectId) {
        String sql = "SELECT seat_number FROM StudentEnrollments "
                + "WHERE section_subject_id = ? AND seat_number IS NOT NULL "
                + "ORDER BY seat_number";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            try (ResultSet rs = ps.executeQuery()) {
                int expected = 1;
                while (rs.next()) {
                    int sn = rs.getInt(1);
                    if (sn != expected)
                        return expected;
                    expected++;
                }
                return expected;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Creates a draft attendance session for a class/date, or reopens it as
     * draft if it already exists.
     */
    public boolean ensureAttendanceSession(int sectionSubjectId, Date date) {
        String sql = "INSERT INTO AttendanceSessions (section_subject_id, date, status) " +
                "VALUES (?, ?, 'draft') " +
                "ON CONFLICT(section_subject_id, date) DO UPDATE SET " +
                "status = CASE WHEN status = 'concluded' THEN status ELSE 'draft' END";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Saves start_time, end_time, and classroom_type on a session row.
     * Uses upsert so the row is created if it doesn't exist yet.
     * Called whenever the teacher types or changes class times.
     */
    public void updateSessionTimes(int sectionSubjectId, Date date,
            String startTime, String endTime, String classroomType) {
        // First ensure the row exists (as draft if new)
        ensureAttendanceSession(sectionSubjectId, date);
        // Then update the times
        String sql = "UPDATE AttendanceSessions SET start_time = ?, end_time = ?, classroom_type = ? " +
                "WHERE section_subject_id = ? AND date = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startTime);
            ps.setString(2, endTime);
            ps.setString(3, classroomType);
            ps.setInt(4, sectionSubjectId);
            ps.setDate(5, date);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Marks an attendance session as concluded for a class/date,
     * and saves the class start/end times and classroom type.
     */
    public boolean markAttendanceSessionConcluded(int sectionSubjectId, Date date,
            String startTime, String endTime, String classroomType) {
        String sql = "INSERT INTO AttendanceSessions (section_subject_id, date, status, start_time, end_time, classroom_type) "
                +
                "VALUES (?, ?, 'concluded', ?, ?, ?) " +
                "ON CONFLICT(section_subject_id, date) DO UPDATE SET " +
                "status = 'concluded', start_time = excluded.start_time, " +
                "end_time = excluded.end_time, classroom_type = excluded.classroom_type";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);
            ps.setString(3, startTime);
            ps.setString(4, endTime);
            ps.setString(5, classroomType);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Backward-compatible overload without times.
     */
    public boolean markAttendanceSessionConcluded(int sectionSubjectId, Date date) {
        return markAttendanceSessionConcluded(sectionSubjectId, date, null, null, null);
    }

    /**
     * Returns saved session times [startTime, endTime, classroomType] for a
     * class/date,
     * or null if no session exists.
     */
    public String[] getSessionTimes(int sectionSubjectId, Date date) {
        String sql = "SELECT start_time, end_time, classroom_type FROM AttendanceSessions " +
                "WHERE section_subject_id = ? AND date = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[] {
                            rs.getString("start_time"),
                            rs.getString("end_time"),
                            rs.getString("classroom_type")
                    };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the session state for a class/date.
     *
     * If the session row is missing but attendance rows exist, the code treats
     * it as draft for backward compatibility with older data.
     */
    public String getAttendanceSessionStatus(int sectionSubjectId, Date date) {
        String sql = "SELECT status FROM AttendanceSessions WHERE section_subject_id = ? AND date = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String attendanceSql = "SELECT 1 FROM Attendance WHERE section_subject_id = ? AND date = ? LIMIT 1";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(attendanceSql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "draft";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns a map of date -> session state for one month.
     *
     * The first query reads AttendanceSessions. The second query finds older
     * Attendance rows without a session row and labels them draft.
     */
    public Map<LocalDate, String> getAttendanceSessionStatesForMonth(int sectionSubjectId, LocalDate month) {
        Map<LocalDate, String> states = new HashMap<>();
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        String sessionSql = "SELECT date, status FROM AttendanceSessions " +
                "WHERE section_subject_id = ? AND date BETWEEN ? AND ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sessionSql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dateStr = rs.getString("date");
                    LocalDate ld = null;
                    try {
                        ld = Date.valueOf(dateStr).toLocalDate();
                    } catch (IllegalArgumentException e) {
                        try {
                            ld = new Date(Long.parseLong(dateStr)).toLocalDate();
                        } catch (NumberFormatException nfe) {
                        }
                    }
                    if (ld != null) {
                        states.put(ld, rs.getString("status"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String attendanceSql = "SELECT DISTINCT date FROM Attendance " +
                "WHERE section_subject_id = ? AND date BETWEEN ? AND ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(attendanceSql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dateStr = rs.getString("date");
                    LocalDate ld = null;
                    try {
                        ld = Date.valueOf(dateStr).toLocalDate();
                    } catch (IllegalArgumentException e) {
                        try {
                            ld = new Date(Long.parseLong(dateStr)).toLocalDate();
                        } catch (NumberFormatException nfe) {
                        }
                    }
                    if (ld != null) {
                        states.putIfAbsent(ld, "draft");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return states;
    }

    /**
     * Deletes one student's attendance mark for a class/date.
     */
    public boolean clearAttendanceMark(String studentId, int sectionSubjectId, Date date) {
        String sql = "DELETE FROM Attendance WHERE student_id = ? AND section_subject_id = ? AND date = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, sectionSubjectId);
            ps.setDate(3, date);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns student_id -> status for all marks on one class/date.
     */
    public Map<String, String> getAttendanceStatusesForDate(int sectionSubjectId, Date date) {
        if (date == null) {
            return new HashMap<>();
        }

        String sql = "SELECT student_id, status FROM Attendance WHERE section_subject_id = ? AND date = ?";
        Map<String, String> statuses = new HashMap<>();

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    statuses.put(rs.getString("student_id"), rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return statuses;
    }

    public Map<String, AttendanceMark> getAttendanceMarksForDate(int sectionSubjectId, Date date) {
        Map<String, AttendanceMark> marks = new HashMap<>();
        if (date == null) {
            return marks;
        }

        String sql = "SELECT student_id, status, time_in, time_out, seat_number " +
                "FROM Attendance WHERE section_subject_id = ? AND date = ?";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setDate(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    marks.put(rs.getString("student_id"), new AttendanceMark(
                            rs.getString("status"),
                            parseTimeString(rs.getString("time_in")),
                            parseTimeString(rs.getString("time_out")),
                            rs.getInt("seat_number")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return marks;
    }

    /**
     * Returns a map of seat_number -> student_name for the classroom visualization.
     * Now reads from SeatAssignments table filtered by classroom type.
     */
    public Map<Integer, String> getSeatOccupants(int sectionSubjectId, String classroomType) {
        Map<Integer, String> occupants = new HashMap<>();
        if (classroomType == null)
            return occupants;

        String sql = "SELECT sa.seat_number, s.first_name, s.last_name " +
                "FROM SeatAssignments sa " +
                "JOIN Students s ON sa.student_id = s.student_id " +
                "WHERE sa.section_subject_id = ? AND sa.classroom_type = ?";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);
            ps.setString(2, classroomType);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("first_name") + " " + rs.getString("last_name");
                    occupants.put(rs.getInt("seat_number"), name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return occupants;
    }

    /**
     * Backward-compatible wrapper. Reads from old StudentEnrollments.seat_number.
     * New code should use getSeatOccupants(ssId, classroomType) instead.
     */
    @Deprecated
    public Map<Integer, String> getPermanentSeatOccupants(int sectionSubjectId) {
        Map<Integer, String> occupants = new HashMap<>();

        String sql = "SELECT se.seat_number, s.first_name, s.last_name " +
                "FROM StudentEnrollments se " +
                "JOIN Students s ON se.student_id = s.student_id " +
                "WHERE se.section_subject_id = ? AND se.seat_number IS NOT NULL";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("first_name") + " " + rs.getString("last_name");
                    occupants.put(rs.getInt("seat_number"), name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return occupants;
    }

    private static Time parseTimeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Time.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            try {
                return new Time(Long.parseLong(value.trim()));
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

    /**
     * Counts Absents per student for one class offering.
     *
     * The return map key is student_id and value is the absent count.
     */
    public Map<String, Integer> getAbsenceCounts(int sectionSubjectId) {
        String sql = "SELECT student_id, COUNT(*) AS absent_count " +
                "FROM Attendance a " +
                "WHERE a.section_subject_id = ? AND a.status = 'Absent' " +
                "AND EXISTS (SELECT 1 FROM AttendanceSessions asess " +
                "  WHERE asess.section_subject_id = a.section_subject_id " +
                "  AND asess.date = a.date AND asess.status = 'concluded') " +
                "GROUP BY student_id";
        Map<String, Integer> counts = new HashMap<>();

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionSubjectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("student_id"), rs.getInt("absent_count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return counts;
    }

    /**
     * Builds rows for the attendance report table.
     *
     * It uses SQL aggregation:
     * - SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) counts Present.
     * - COALESCE turns NULL into 0 when a student has no attendance yet.
     * - CASE computes Good Standing/Warning/FDA from absent count.
     */
    public Object[][] getAttendanceRows(int sectionSubjectId) {
        String sql = "SELECT " +
                "  s.student_id, " +
                "  TRIM(s.last_name || ', ' || s.first_name || ' ' || COALESCE(s.middle_initial, '')) AS full_name, " +
                "  COALESCE(SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END), 0) AS present, " +
                "  COALESCE(SUM(CASE WHEN a.status = 'Late' THEN 1 ELSE 0 END), 0) AS late, " +
                "  COALESCE(SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END), 0) AS absent, " +
                "  COALESCE(SUM(CASE WHEN a.status = 'Excused' THEN 1 ELSE 0 END), 0) AS excused, " +
                "  CASE " +
                "    WHEN COALESCE(SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END), 0) >= 3 THEN 'FDA' " +
                "    WHEN COALESCE(SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END), 0) >= 2 THEN 'Warning' " +
                "    ELSE 'Good Standing' " +
                "  END AS standing " +
                "FROM StudentEnrollments se " +
                "JOIN Students s ON se.student_id = s.student_id " +
                "LEFT JOIN Attendance a ON a.student_id = se.student_id " +
                "  AND a.section_subject_id = se.section_subject_id " +
                "  AND EXISTS (SELECT 1 FROM AttendanceSessions asess " +
                "    WHERE asess.section_subject_id = a.section_subject_id " +
                "    AND asess.date = a.date AND asess.status = 'concluded') " +
                "WHERE se.section_subject_id = ? " +
                "GROUP BY s.student_id, s.last_name, s.first_name, s.middle_initial " +
                "ORDER BY s.last_name, s.first_name";

        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sectionSubjectId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new Object[] {
                            rows.size() + 1,
                            rs.getString("student_id"),
                            rs.getString("full_name"),
                            rs.getInt("present"),
                            rs.getInt("late"),
                            rs.getInt("absent"),
                            rs.getInt("excused"),
                            rs.getString("standing")
                    });
                }
                return rows.toArray(new Object[0][0]);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return new Object[0][0];
        }
    }

    /**
     * Finds the email connected to an RFID/card UID.
     *
     * Teachers and Admins can scan, so UNION searches both tables and returns
     * the first matching email.
     */
    public String getEmailByUID(String uid) {
        String sql = "SELECT email FROM Teachers WHERE uid = ? UNION SELECT email FROM Admins WHERE uid = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, uid);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("email");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class UserLoginInfo {
        public final String email;
        public final String role;

        public UserLoginInfo(String email, String role) {
            this.email = email;
            this.role = role;
        }
    }

    /**
     * Gets both email and role in a single query using UID.
     */
    public UserLoginInfo getUserLoginInfoByUID(String uid) {
        String sql = "SELECT u.email, u.role FROM Users u JOIN Teachers t ON u.user_id = t.user_id WHERE t.uid = ? " +
                "UNION " +
                "SELECT u.email, u.role FROM Users u JOIN Admins a ON u.user_id = a.user_id WHERE a.uid = ?";

        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, uid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new UserLoginInfo(rs.getString("email"), rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Links an existing Students row to a newly created Users account.
    public boolean linkStudentUserId(String studentId, int userId) {
        String sql = "UPDATE Students SET user_id = ? WHERE student_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getStudentIdByUID(String uid) {
        String sql = "SELECT student_id FROM Students WHERE uid = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uid);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("student_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static final class AttendanceMark {
        public final String status;
        public final Time timeIn;
        public final Time timeOut;
        public final int seatNumber;

        public AttendanceMark(String status, Time timeIn, Time timeOut, int seatNumber) {
            this.status = status;
            this.timeIn = timeIn;
            this.timeOut = timeOut;
            this.seatNumber = seatNumber;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Decides where seatendance.db should live.
     *
     * The app might be launched from different working directories, so this
     * searches for the Maven module and then appends seatendance.db.
     */
    private static Path resolveDatabasePath() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path moduleDir = findModuleDirectory(cwd);
        return moduleDir.resolve("seatendance.db").toAbsolutePath().normalize();
    }

    /**
     * Walks upward from the start path until it finds a folder that looks like
     * the Maven module, meaning it contains pom.xml.
     */
    private static Path findModuleDirectory(Path start) {
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("pom.xml"))) {
                return dir;
            }

            Path lowercaseNested = dir.resolve("seatendance");
            if (Files.exists(lowercaseNested.resolve("pom.xml"))) {
                return lowercaseNested;
            }

            Path repoNested = dir.resolve("Seattendance").resolve("seatendance");
            if (Files.exists(repoNested.resolve("pom.xml"))) {
                return repoNested;
            }
        }

        return start;
    }

    /**
     * Makes sure the database file's parent folder exists before SQLite opens
     * the file.
     */
    private static void ensureDatabaseDirectory() throws SQLException {
        Path parent = DB_PATH.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (java.io.IOException e) {
            throw new SQLException("Unable to create SQLite database directory: " + parent, e);
        }
    }

    /**
     * Helper for nullable INTEGER columns.
     *
     * PreparedStatement#setInt cannot store SQL NULL, so nullable Integer
     * values must call setNull when the Java value is null.
     */
    private void setNullableInt(PreparedStatement ps, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.INTEGER);
        } else {
            ps.setInt(parameterIndex, value);
        }
    }

    // -----------------------------------------------------------------------
    // DDL
    // -----------------------------------------------------------------------

    private static final String CREATE_USERS = "CREATE TABLE IF NOT EXISTS Users (" +
            "  user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  email TEXT NOT NULL UNIQUE," +
            "  password_hash TEXT NOT NULL," +
            "  role TEXT NOT NULL CHECK (role IN ('admin', 'teacher', 'student'))," +
            "  created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
            ")";

    private static final String CREATE_ADMINS = "CREATE TABLE IF NOT EXISTS Admins (" +
            "  admin_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  uid TEXT UNIQUE," +
            "  user_id INTEGER NOT NULL," +
            "  first_name TEXT NOT NULL," +
            "  last_name TEXT NOT NULL," +
            "  middle_initial TEXT," +
            "  email TEXT NOT NULL," +
            "  FOREIGN KEY (user_id) REFERENCES Users(user_id)" +
            ")";

    private static final String CREATE_COURSES = "CREATE TABLE IF NOT EXISTS Courses (" +
            "  course TEXT NOT NULL," +
            "  course_code TEXT NOT NULL," +
            "  PRIMARY KEY (course)" +
            ")";

    private static final String CREATE_TEACHERS = "CREATE TABLE IF NOT EXISTS Teachers (" +
            "  uid TEXT NOT NULL," +
            "  teacher_id TEXT NOT NULL," +
            "  user_id INTEGER NOT NULL," +
            "  first_name TEXT NOT NULL," +
            "  last_name TEXT NOT NULL," +
            "  middle_initial TEXT," +
            "  email TEXT NOT NULL," +
            "  PRIMARY KEY (teacher_id)," +
            "  UNIQUE (uid)," +
            "  UNIQUE (user_id)," +
            "  UNIQUE (email)," +
            "  FOREIGN KEY (user_id) REFERENCES Users(user_id)" +
            "  )";

    // course_code stored here so the generated section_name can use it
    // without needing a JOIN. Result: "BSIT 2 YA-1" instead of the full name.
    private static final String CREATE_SECTIONS = "CREATE TABLE IF NOT EXISTS Sections (" +
            "  section_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  course TEXT NOT NULL," +
            "  course_code TEXT NOT NULL," +
            "  year INTEGER NOT NULL CHECK (year BETWEEN 1 AND 6)," +
            "  semester TEXT NOT NULL CHECK (semester IN ('A', 'B', 'C'))," +
            "  section_number INTEGER NOT NULL," +
            "  section_name TEXT GENERATED ALWAYS AS " +
            "    (course_code || ' ' || year || ' Y' || semester || '-' || section_number) STORED," +
            "  FOREIGN KEY (course) REFERENCES Courses(course)," +
            "  UNIQUE (course, year, semester, section_number)" +
            ")";

    private static final String CREATE_STUDENTS = "CREATE TABLE IF NOT EXISTS Students (" +
            "  uid TEXT NOT NULL," +
            "  student_id TEXT NOT NULL," +
            "  user_id INTEGER NULL," +
            "  first_name TEXT NOT NULL," +
            "  last_name TEXT NOT NULL," +
            "  middle_initial TEXT," +
            "  email TEXT NOT NULL," +
            "  section_id INTEGER," +
            "  is_irregular INTEGER NOT NULL DEFAULT 0," +
            "  PRIMARY KEY (student_id)," +
            "  UNIQUE (uid)," +
            "  UNIQUE (email)," +
            "  UNIQUE (user_id)," +
            "  FOREIGN KEY (user_id) REFERENCES Users(user_id)," +
            "  FOREIGN KEY (section_id) REFERENCES Sections(section_id)" +
            ")";

    private static final String CREATE_SUBJECTS = "CREATE TABLE IF NOT EXISTS Subjects (" +
            "  subject_code TEXT NOT NULL," +
            "  subject_name TEXT NOT NULL," +
            "  subject_type TEXT NOT NULL CHECK (subject_type IN ('Professional', 'Minor'))," +
            "  course TEXT NOT NULL," +
            "  PRIMARY KEY (subject_code, course)," +
            "  FOREIGN KEY (course) REFERENCES Courses(course)" +
            ")";

    private static final String CREATE_SECTION_SUBJECTS = "CREATE TABLE IF NOT EXISTS SectionSubjects (" +
            "  section_subject_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  section_id INTEGER NOT NULL," +
            "  subject_code TEXT NOT NULL," +
            "  course TEXT NOT NULL," +
            "  teacher_id TEXT NOT NULL," +
            "  FOREIGN KEY (section_id) REFERENCES Sections(section_id)," +
            "  FOREIGN KEY (subject_code, course) REFERENCES Subjects(subject_code, course)," +
            "  FOREIGN KEY (teacher_id) REFERENCES Teachers(teacher_id)," +
            "  UNIQUE (section_id, subject_code)" +
            ")";

    private static final String CREATE_STUDENT_ENROLLMENTS = "CREATE TABLE IF NOT EXISTS StudentEnrollments (" +
            "  student_id TEXT NOT NULL," +
            "  section_subject_id INTEGER NOT NULL," +
            "  seat_number INTEGER," +
            "  PRIMARY KEY (student_id, section_subject_id)," +
            "  FOREIGN KEY (student_id) REFERENCES Students(student_id)," +
            "  FOREIGN KEY (section_subject_id) REFERENCES SectionSubjects(section_subject_id)" +
            ")";

    private static final String CREATE_ATTENDANCE_SESSIONS = "CREATE TABLE IF NOT EXISTS AttendanceSessions (" +
            "  section_subject_id INTEGER NOT NULL," +
            "  date TEXT NOT NULL," +
            "  status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'concluded'))," +
            "  updated_at TEXT DEFAULT CURRENT_TIMESTAMP," +
            "  PRIMARY KEY (section_subject_id, date)," +
            "  FOREIGN KEY (section_subject_id) REFERENCES SectionSubjects(section_subject_id)" +
            ")";

    private static final String CREATE_ATTENDANCE = "CREATE TABLE IF NOT EXISTS Attendance (" +
            "  attendance_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  student_id TEXT NOT NULL," +
            "  section_subject_id INTEGER NOT NULL," +
            "  date TEXT NOT NULL," +
            "  time_in TEXT," +
            "  time_out TEXT," +
            "  status TEXT NOT NULL DEFAULT 'Absent' CHECK (status IN ('Present', 'Absent', 'Late', 'Excused'))," +
            "  seat_number INTEGER," +
            "  FOREIGN KEY (student_id, section_subject_id) " +
            "    REFERENCES StudentEnrollments(student_id, section_subject_id)," +
            "  UNIQUE (student_id, section_subject_id, date)" +
            ")";

    private static final String CREATE_SEAT_ASSIGNMENTS = "CREATE TABLE IF NOT EXISTS SeatAssignments (" +
            "  student_id TEXT NOT NULL," +
            "  section_subject_id INTEGER NOT NULL," +
            "  classroom_type TEXT NOT NULL," +
            "  seat_number INTEGER NOT NULL," +
            "  PRIMARY KEY (student_id, section_subject_id, classroom_type)," +
            "  FOREIGN KEY (student_id) REFERENCES Students(student_id)," +
            "  FOREIGN KEY (section_subject_id) REFERENCES SectionSubjects(section_subject_id)" +
            ")";

    private static final String CREATE_SECTIONS_NAME_INDEX = "CREATE INDEX IF NOT EXISTS idx_sections_section_name ON Sections(section_name)";

    /**
     * Builds all class options for the attendance report dropdown.
     */
    public List<AttendanceReport.ClassOption> getClassOptions() {
        String sql = "SELECT ss.section_subject_id, sec.section_name, ss.subject_code, ss.course, " +
                "sub.subject_name, t.last_name || ', ' || t.first_name AS teacher_name " +
                "FROM SectionSubjects ss " +
                "JOIN Sections sec ON ss.section_id = sec.section_id " +
                "JOIN Subjects sub ON ss.subject_code = sub.subject_code AND ss.course = sub.course " +
                "JOIN Teachers t ON ss.teacher_id = t.teacher_id " +
                "ORDER BY sec.section_name, ss.subject_code";

        List<AttendanceReport.ClassOption> list = new ArrayList<>();
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new AttendanceReport.ClassOption(
                        rs.getInt("section_subject_id"),
                        rs.getString("section_name"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("teacher_name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Builds class options for the current user.
     *
     * Teachers only see their assigned classes. Admins and non-teacher roles
     * fall back to getClassOptions(), which returns everything.
     */
    public List<AttendanceReport.ClassOption> getClassOptionsForUser(String email) {
        String role = getUserRole(email);
        if (!"teacher".equalsIgnoreCase(role)) {
            return getClassOptions();
        }

        String sql = "SELECT ss.section_subject_id, sec.section_name, ss.subject_code, ss.course, " +
                "sub.subject_name, t.last_name || ', ' || t.first_name AS teacher_name " +
                "FROM SectionSubjects ss " +
                "JOIN Sections sec ON ss.section_id = sec.section_id " +
                "JOIN Subjects sub ON ss.subject_code = sub.subject_code AND ss.course = sub.course " +
                "JOIN Teachers t ON ss.teacher_id = t.teacher_id " +
                "JOIN Users u ON t.user_id = u.user_id " +
                "WHERE u.email = ? " +
                "ORDER BY sec.section_name, ss.subject_code";

        List<AttendanceReport.ClassOption> list = new ArrayList<>();
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AttendanceReport.ClassOption(
                            rs.getInt("section_subject_id"),
                            rs.getString("section_name"),
                            rs.getString("subject_code"),
                            rs.getString("subject_name"),
                            rs.getString("teacher_name")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}