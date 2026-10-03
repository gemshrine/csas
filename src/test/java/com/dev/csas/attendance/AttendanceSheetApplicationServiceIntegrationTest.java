package com.dev.csas.attendance;

import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.LectureID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.api.attendance.AttendanceSheetDetailsResponse;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.control.UnauthorizedTeacherException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.docker.compose.skip.in-tests=true"
)
@Transactional
class AttendanceSheetApplicationServiceIntegrationTest {

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
    private AttendanceSheetApplicationService service;

    @Autowired
    private AttendanceSheetQueryService queries;

    @Autowired
    private AttendanceSheetRepository sheets;

    @Autowired
    private AttendanceSheetMapper mapper;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsLecturesAttendanceAndSheetStatus() {
        Fixture fixture = insertFixture();

        LectureID lecture = service.addLecture(
                fixture.sheetId(),
                new TeacherID(fixture.lectureTeacherId()),
                LocalDate.of(2026, 10, 5),
                "Введение"
        );
        service.registerAttendance(
                fixture.sheetId(),
                new TeacherID(fixture.lectureTeacherId()),
                new StudentID(fixture.firstStudentId()),
                AttendanceStatusEnum.PRESENT,
                lecture
        );
        service.registerAttendance(
                fixture.sheetId(),
                new TeacherID(fixture.lectureTeacherId()),
                new StudentID(fixture.secondStudentId()),
                AttendanceStatusEnum.ABSENT,
                lecture
        );
        service.close(fixture.sheetId(), new TeacherID(fixture.lectureTeacherId()));
        service.archive(fixture.sheetId(), new DeanID(1));

        entityManager.flush();
        entityManager.clear();

        AttendanceSheet reloaded = mapper.toDomain(sheets.findById(fixture.sheetId()).orElseThrow());
        Map<StudentID, AttendanceRecord> records = reloaded.getAttendances(lecture);

        assertEquals(AttendanceSheetStatus.ARCHIVED, reloaded.status());
        assertEquals(1, reloaded.lectures().size());
        assertEquals("Введение", reloaded.lectures().getFirst().getTitle());
        assertEquals(AttendanceStatusEnum.PRESENT, records.get(new StudentID(fixture.firstStudentId())).status());
        assertEquals(AttendanceStatusEnum.ABSENT, records.get(new StudentID(fixture.secondStudentId())).status());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM lectures WHERE attendance_sheet_id = ? AND id = ?",
                Integer.class,
                fixture.sheetId(),
                lecture.id()
        ));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM lecture_attendance WHERE lecture_id = ?",
                Integer.class,
                lecture.id()
        ));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackLectureInsertWhenDomainValidationFails() {
        Fixture fixture = insertFixture();

        assertThrows(UnauthorizedTeacherException.class, () -> service.addLecture(
                fixture.sheetId(),
                new TeacherID(fixture.seminarTeacherId()),
                LocalDate.of(2026, 10, 5),
                "Несанкционированная лекция"
        ));

        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM lectures WHERE attendance_sheet_id = ?",
                Integer.class,
                fixture.sheetId()
        ));
    }

    @Test
    void readsRosterLecturesAndUnmarkedAttendanceOnlyForLectureTeacher() {
        Fixture fixture = insertFixture();
        LectureID lecture = service.addLecture(
                fixture.sheetId(),
                new TeacherID(fixture.lectureTeacherId()),
                LocalDate.of(2026, 10, 5),
                "Введение"
        );

        AttendanceSheetDetailsResponse result = queries.getForTeacher(
                fixture.sheetId(),
                new TeacherID(fixture.lectureTeacherId())
        );

        assertEquals(2, result.students().size());
        assertEquals(1, result.lectures().size());
        assertEquals(lecture.id(), result.lectures().getFirst().id());
        assertEquals(2, result.lectures().getFirst().attendance().size());
        assertNull(result.lectures().getFirst().attendance().getFirst().status());
        assertThrows(ActorAccessDeniedException.class, () -> queries.getForTeacher(
                fixture.sheetId(),
                new TeacherID(fixture.seminarTeacherId())
        ));
    }

    private Fixture insertFixture() {
        long groupId = insert(
                "INSERT INTO groups (name, year_of_admission, specialty, course, education_form, study_form) " +
                        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                "IT-42-test", 2024, "Информатика", 3, "BUDGET", "FULL_TIME"
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
        long firstStudentId = insert(
                "INSERT INTO students (first_name, last_name, record_book_number, group_id) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                "Анна", "Сидорова", "TEST0002", groupId
        );
        long secondStudentId = insert(
                "INSERT INTO students (first_name, last_name, record_book_number, group_id) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                "Иван", "Петров", "TEST0003", groupId
        );
        long disciplineId = insert(
                "INSERT INTO disciplines (name, lecture_hours, practice_hours, lab_hours, control_form) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING id",
                "Дисциплина интеграционного теста посещаемости", 16, 32, 16, "EXAM"
        );
        long semesterId = insert(
                "INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "Тестовый семестр", Date.valueOf("2026-09-01"), Date.valueOf("2027-01-31")
        );
        long seminarCourseId = insert(
                "INSERT INTO courses " +
                        "(discipline_id, group_id, start_semester_id, end_semester_id, course_type, teacher_id, status) " +
                        "VALUES (?, ?, ?, ?, 'SEMINAR', ?, 'IN_PROGRESS') RETURNING id",
                disciplineId, groupId, semesterId, semesterId, seminarTeacherId
        );
        long lectureCourseId = insert(
                "INSERT INTO courses " +
                        "(discipline_id, group_id, start_semester_id, end_semester_id, course_type, teacher_id, status) " +
                        "VALUES (?, ?, ?, ?, 'LECTURE', ?, 'IN_PROGRESS') RETURNING id",
                disciplineId, groupId, semesterId, semesterId, lectureTeacherId
        );
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)", seminarCourseId, firstStudentId);
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)", seminarCourseId, secondStudentId);
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)", lectureCourseId, firstStudentId);
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)", lectureCourseId, secondStudentId);
        long sheetId = insert(
                "INSERT INTO attendance_sheets (course_id, status) VALUES (?, 'OPEN') RETURNING id",
                lectureCourseId
        );

        return new Fixture(sheetId, firstStudentId, secondStudentId, seminarTeacherId, lectureTeacherId);
    }

    private long insert(String sql, Object... parameters) {
        Long id = jdbc.queryForObject(sql, Long.class, parameters);
        if (id == null) {
            throw new IllegalStateException("Вставка не вернула идентификатор");
        }
        return id;
    }

    private record Fixture(
            long sheetId,
            long firstStudentId,
            long secondStudentId,
            long seminarTeacherId,
            long lectureTeacherId
    ) {
    }
}
