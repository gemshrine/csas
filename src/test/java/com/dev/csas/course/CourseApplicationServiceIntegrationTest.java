package com.dev.csas.course;

import com.dev.csas.api.course.CourseWorkRequest;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.docker.compose.skip.in-tests=true")
@Transactional
class CourseApplicationServiceIntegrationTest {

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
    private CourseApplicationService service;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void addsWorkScoresStudentAndCalculatesRating() {
        Fixture fixture = insertFixture();
        TeacherID teacher = new TeacherID(fixture.seminarTeacherId());

        long workNumber = service.addWork(fixture.courseId(), teacher,
                new CourseWorkRequest("Практическая работа", 40, Date.valueOf("2026-10-10").toLocalDate()));
        service.enterScore(fixture.courseId(), workNumber, fixture.studentId(), teacher, 30);
        entityManager.flush();

        var details = service.getForTeacher(fixture.courseId(), teacher);
        assertEquals(1, details.works().size());
        assertEquals(30, details.works().getFirst().scores().getFirst().score());
        assertEquals(75.0, details.students().getFirst().rating());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM course_works WHERE course_id = ?", Integer.class,
                fixture.courseId()));
    }

    @Test
    void lectureTeacherCannotReadSeminarResults() {
        Fixture fixture = insertFixture();

        assertThrows(ActorAccessDeniedException.class, () -> service.getForTeacher(
                fixture.courseId(), new TeacherID(fixture.lectureTeacherId())));
    }

    @Test
    void completionRequiresAtLeastOneWorkAndPersistsStatus() {
        Fixture fixture = insertFixture();
        TeacherID teacher = new TeacherID(fixture.seminarTeacherId());
        assertThrows(CourseWithoutWorksException.class, () -> service.complete(fixture.courseId(), teacher));

        service.addWork(fixture.courseId(), teacher,
                new CourseWorkRequest("Работа", 10, Date.valueOf("2026-10-10").toLocalDate()));
        service.complete(fixture.courseId(), teacher);

        assertEquals(CourseStatus.COMPLETED, service.getForTeacher(fixture.courseId(), teacher).status());
    }

    private Fixture insertFixture() {
        long groupId = insert("INSERT INTO groups (name, year_of_admission, specialty, course, education_form, study_form) " +
                "VALUES (?, 2024, ?, 3, 'BUDGET', 'FULL_TIME') RETURNING id", "API-TEST", "Информатика");
        long seminarTeacherId = insert("INSERT INTO teachers (first_name, last_name, department, position) " +
                "VALUES ('Иван', 'Иванов', 'Информатика', 'Преподаватель') RETURNING id");
        long lectureTeacherId = insert("INSERT INTO teachers (first_name, last_name, department, position) " +
                "VALUES ('Пётр', 'Петров', 'Информатика', 'Преподаватель') RETURNING id");
        long studentId = insert("INSERT INTO students (first_name, last_name, record_book_number, group_id) " +
                "VALUES ('Анна', 'Сидорова', 'API00001', ?) RETURNING id", groupId);
        long disciplineId = insert("INSERT INTO disciplines (name, lecture_hours, practice_hours, lab_hours, control_form) " +
                "VALUES ('API test discipline', 16, 32, 16, 'EXAM') RETURNING id");
        long semesterId = insert("INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "API test semester", Date.valueOf("2026-09-01"), Date.valueOf("2027-01-31"));
        long courseId = insert("INSERT INTO courses (discipline_id, group_id, semester_id, seminar_teacher_id, " +
                        "lecture_teacher_id, status) VALUES (?, ?, ?, ?, ?, 'IN_PROGRESS') RETURNING id",
                disciplineId, groupId, semesterId, seminarTeacherId, lectureTeacherId);
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)", courseId, studentId);
        return new Fixture(courseId, studentId, seminarTeacherId, lectureTeacherId);
    }

    private long insert(String sql, Object... parameters) {
        Long id = jdbc.queryForObject(sql, Long.class, parameters);
        assertNotNull(id);
        return id;
    }

    private record Fixture(long courseId, long studentId, long seminarTeacherId, long lectureTeacherId) {
    }
}
