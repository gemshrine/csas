package com.dev.csas.course;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class CourseTypeMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("csas_migration_test")
            .withUsername("csas")
            .withPassword("csas");

    @Test
    void splitsExistingCourseAndMovesAttendanceToLectureCourse() throws SQLException {
        migrateToPreviousVersion();

        long groupId = insert("INSERT INTO groups " +
                "(name, year_of_admission, specialty, course, education_form, study_form) " +
                "VALUES ('MIGRATION-TEST', 2024, 'Информатика', 3, 'BUDGET', 'FULL_TIME') RETURNING id");
        long seminarTeacherId = insert("INSERT INTO teachers " +
                "(first_name, last_name, department, position) " +
                "VALUES ('Анна', 'Семинарова', 'ИВТ', 'Преподаватель') RETURNING id");
        long lectureTeacherId = insert("INSERT INTO teachers " +
                "(first_name, last_name, department, position) " +
                "VALUES ('Иван', 'Лекторов', 'ИВТ', 'Преподаватель') RETURNING id");
        long studentId = insert("INSERT INTO students " +
                "(first_name, last_name, record_book_number, group_id) " +
                "VALUES ('Пётр', 'Студентов', 'MIGR0001', " + groupId + ") RETURNING id");
        long disciplineId = insert("INSERT INTO disciplines " +
                "(name, lecture_hours, practice_hours, lab_hours, control_form) " +
                "VALUES ('Миграционная дисциплина', 16, 32, 0, 'EXAM') RETURNING id");
        long semesterId = insert("INSERT INTO semesters (name, start_date, end_date) " +
                "VALUES ('Миграционный семестр', DATE '2026-09-01', DATE '2027-01-31') RETURNING id");
        long oldCourseId = insert("INSERT INTO courses " +
                "(discipline_id, group_id, semester_id, seminar_teacher_id, lecture_teacher_id, status) " +
                "VALUES (" + disciplineId + ", " + groupId + ", " + semesterId + ", " + seminarTeacherId +
                ", " + lectureTeacherId + ", 'COMPLETED') RETURNING id");
        execute("INSERT INTO course_students (course_id, student_id) VALUES (" + oldCourseId + ", " + studentId + ")");
        execute("INSERT INTO course_works (course_id, number, title, max_score, work_date) VALUES (" + oldCourseId +
                ", 1, 'Работа', 20, DATE '2026-10-01')");
        long controlSheetId = insert("INSERT INTO control_sheets " +
                "(course_id, control_form, status, grade_5_from, grade_4_from, grade_3_from) VALUES (" +
                oldCourseId + ", 'EXAM', 'CLOSED', 90, 80, 70) RETURNING id");
        long attendanceSheetId = insert("INSERT INTO attendance_sheets (course_id, status) VALUES (" +
                oldCourseId + ", 'CLOSED') RETURNING id");

        migrateToLatestVersion();

        long seminarCourseId = findCourseId(disciplineId, "SEMINAR");
        long lectureCourseId = findCourseId(disciplineId, "LECTURE");
        assertEquals(oldCourseId, seminarCourseId);
        assertEquals(seminarTeacherId, findTeacherId(seminarCourseId));
        assertEquals(lectureTeacherId, findTeacherId(lectureCourseId));
        assertEquals("COMPLETED", findCourseStatus(seminarCourseId));
        assertEquals("IN_PROGRESS", findCourseStatus(lectureCourseId));
        assertEquals(1, count("SELECT COUNT(*) FROM course_students WHERE course_id = " + seminarCourseId));
        assertEquals(1, count("SELECT COUNT(*) FROM course_students WHERE course_id = " + lectureCourseId));
        assertEquals(1, count("SELECT COUNT(*) FROM course_works WHERE course_id = " + seminarCourseId));
        assertEquals(0, count("SELECT COUNT(*) FROM course_works WHERE course_id = " + lectureCourseId));
        assertEquals(seminarCourseId, findRelatedCourseId(
                "SELECT course_id FROM control_sheets WHERE id = " + controlSheetId));
        assertEquals(lectureCourseId, findRelatedCourseId(
                "SELECT course_id FROM attendance_sheets WHERE id = " + attendanceSheetId));
    }

    private void migrateToPreviousVersion() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("202609271200"))
                .load()
                .migrate();
    }

    private void migrateToLatestVersion() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private long insert(String sql) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private long findCourseId(long disciplineId, String type) throws SQLException {
        return findLong("SELECT id FROM courses WHERE discipline_id = " + disciplineId +
                " AND course_type = '" + type + "'");
    }

    private long findTeacherId(long courseId) throws SQLException {
        return findLong("SELECT teacher_id FROM courses WHERE id = " + courseId);
    }

    private String findCourseStatus(long courseId) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT status FROM courses WHERE id = " + courseId);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getString(1);
        }
    }

    private long findRelatedCourseId(String sql) throws SQLException {
        return findLong(sql);
    }

    private long count(String sql) throws SQLException {
        return findLong(sql);
    }

    private long findLong(String sql) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
