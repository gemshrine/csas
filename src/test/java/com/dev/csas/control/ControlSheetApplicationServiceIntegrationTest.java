package com.dev.csas.control;

import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.course.CourseQueryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.docker.compose.skip.in-tests=true"
)
@Transactional
class ControlSheetApplicationServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("csas_test")
            .withUsername("csas")
            .withPassword("csas");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ControlSheetApplicationService service;

    @Autowired
    private ControlSheetQueryService queries;

    @Autowired
    private CourseQueryService courseQueries;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void enteringResultPersistsResultAndAuditEvent() {
        Fixture fixture = insertFixture();

        service.enterNumericResult(
                fixture.sheetId(),
                new TeacherID(fixture.seminarTeacherId()),
                new StudentID(fixture.studentId()),
                88,
                "Пересдача"
        );
        entityManager.flush();

        Integer score = jdbc.queryForObject(
                "SELECT numeric_score FROM control_results WHERE control_sheet_id = ? AND student_id = ?",
                Integer.class,
                fixture.sheetId(),
                fixture.studentId()
        );
        Integer eventCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM control_sheet_events " +
                        "WHERE control_sheet_id = ? AND event_type = 'RESULT_ENTERED' " +
                        "AND student_id = ? AND actor_id = ? AND reason = ?",
                Integer.class,
                fixture.sheetId(),
                fixture.studentId(),
                fixture.seminarTeacherId(),
                "Пересдача"
        );

        assertEquals(88, score);
        assertEquals(1, eventCount);
    }

    @Test
    void readsControlResultsAndOnlyAllowsAssignedSeminarTeacher() {
        Fixture fixture = insertFixture();

        ControlSheetDetailsResponse result = queries.getForTeacher(
                fixture.sheetId(),
                new TeacherID(fixture.seminarTeacherId())
        );

        assertEquals(1, result.students().size());
        assertNull(result.students().getFirst().numericScore());
        assertNull(result.students().getFirst().passFail());
        assertThrows(ActorAccessDeniedException.class, () -> queries.getForTeacher(
                fixture.sheetId(),
                new TeacherID(fixture.seminarTeacherId() + 100)
        ));

        service.enterNumericResult(
                fixture.sheetId(),
                new TeacherID(fixture.seminarTeacherId()),
                new StudentID(fixture.studentId()),
                88,
                null
        );
        assertEquals(88, queries.getForTeacher(
                fixture.sheetId(),
                new TeacherID(fixture.seminarTeacherId())
        ).students().getFirst().numericScore());
    }

    @Test
    void listsOnlyAssignedCoursesForTeacherAndAllCoursesForDean() {
        Fixture fixture = insertFixture();

        var seminarCourses = courseQueries.listForTeacher(new TeacherID(fixture.seminarTeacherId()));
        var lectureCourses = courseQueries.listForTeacher(new TeacherID(fixture.lectureTeacherId()));
        var deanCourses = courseQueries.listForDean();

        assertEquals(1, seminarCourses.size());
        assertEquals(fixture.sheetId(), seminarCourses.getFirst().controlSheetId());
        assertEquals(true, seminarCourses.getFirst().canReadControlSheet());
        assertEquals(false, seminarCourses.getFirst().canReadAttendanceSheet());
        assertEquals(1, lectureCourses.size());
        assertNull(lectureCourses.getFirst().controlSheetId());
        assertEquals(false, lectureCourses.getFirst().canReadControlSheet());
        assertEquals(true, lectureCourses.getFirst().canReadAttendanceSheet());
        assertEquals(1, deanCourses.size());
        assertEquals(true, deanCourses.getFirst().canReadControlSheet());
        assertEquals(true, deanCourses.getFirst().canReadAttendanceSheet());
    }

    private Fixture insertFixture() {
        long groupId = insert(
                "INSERT INTO groups (name, year_of_admission, specialty, course, education_form, study_form) " +
                        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                "IT-41-test", 2024, "Информатика", 3, "BUDGET", "FULL_TIME"
        );
        long seminarTeacherId = insert(
                "INSERT INTO teachers (first_name, last_name, department, position) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                "Иван", "Иванов", "Информатика", "Преподаватель"
        );
        long lectureTeacherId = insert(
                "INSERT INTO teachers (first_name, last_name, department, position) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                "Пётр", "Петров", "Информатика", "Преподаватель"
        );
        long studentId = insert(
                "INSERT INTO students (first_name, last_name, record_book_number, group_id) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                "Анна", "Сидорова", "TEST0001", groupId
        );
        long disciplineId = insert(
                "INSERT INTO disciplines (name, lecture_hours, practice_hours, lab_hours, control_form) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING id",
                "Дисциплина интеграционного теста", 16, 32, 16, "EXAM"
        );
        long semesterId = insert(
                "INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "Тестовый семестр", Date.valueOf("2026-09-01"), Date.valueOf("2027-01-31")
        );
        long courseId = insert(
                "INSERT INTO courses " +
                        "(discipline_id, group_id, semester_id, seminar_teacher_id, lecture_teacher_id, status) " +
                        "VALUES (?, ?, ?, ?, ?, 'COMPLETED') RETURNING id",
                disciplineId, groupId, semesterId, seminarTeacherId, lectureTeacherId
        );
        jdbc.update(
                "INSERT INTO course_students (course_id, student_id) VALUES (?, ?)",
                courseId,
                studentId
        );
        long workId = insert(
                "INSERT INTO course_works (course_id, number, title, max_score, work_date) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING id",
                courseId, 1, "Работа", 100, Date.valueOf("2026-10-01")
        );
        jdbc.update(
                "INSERT INTO course_scores (work_id, student_id, score) VALUES (?, ?, ?)",
                workId,
                studentId,
                60
        );
        long sheetId = insert(
                "INSERT INTO control_sheets " +
                        "(course_id, control_form, status, grade_5_from, grade_4_from, grade_3_from) " +
                        "VALUES (?, 'EXAM', 'OPEN', 90, 80, 70) RETURNING id",
                courseId
        );

        return new Fixture(sheetId, studentId, seminarTeacherId, lectureTeacherId);
    }

    private long insert(String sql, Object... parameters) {
        Long id = jdbc.queryForObject(sql, Long.class, parameters);
        if (id == null) {
            throw new IllegalStateException("Вставка не вернула идентификатор");
        }
        return id;
    }

    private record Fixture(long sheetId, long studentId, long seminarTeacherId, long lectureTeacherId) {
    }
}
