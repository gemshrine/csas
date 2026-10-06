package com.dev.csas.course;

import com.dev.csas.api.course.CourseWorkRequest;
import com.dev.csas.api.course.CourseCreationRequest;
import com.dev.csas.api.user.UserCreationRequest;
import com.dev.csas.coursework.CourseworkSheetApplicationService;
import com.dev.csas.coursework.CourseworkSheetQueryService;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.coursework.CourseworkGrade;
import com.dev.csas.control.ControlSheetApplicationService;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.control.GradingScale;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserAdministrationService;
import com.dev.csas.user.UserRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Date;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
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
    private CourseAssignmentService courseAssignment;

    @Autowired
    private UserAdministrationService userAdministration;

    @Autowired
    private UserRepository users;

    @Autowired
    private StudentRepository students;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CourseworkSheetRepository courseworkSheets;

    @Autowired
    private CourseworkSheetApplicationService courseworkApplication;

    @Autowired
    private CourseworkSheetQueryService courseworkQueries;

    @Autowired
    private ControlSheetApplicationService controlSheets;

    @Autowired
    private StudentCourseQueryService studentCourses;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private FindByIndexNameSessionRepository<? extends Session> sessions;

    @Test
    void addsWorkScoresStudentAndCalculatesRating() {
        Fixture fixture = insertFixture();
        TeacherID teacher = new TeacherID(fixture.seminarTeacherId());

        long workNumber = service.addWork(fixture.seminarCourseId(), teacher,
                new CourseWorkRequest("Практическая работа", 40, Date.valueOf("2026-10-10").toLocalDate()));
        service.enterScore(fixture.seminarCourseId(), workNumber, fixture.studentId(), teacher, 30);
        entityManager.flush();

        var details = service.getForTeacher(fixture.seminarCourseId(), teacher);
        assertEquals(CourseType.SEMINAR, details.courseType());
        assertEquals(fixture.disciplineId(), details.disciplineId());
        assertEquals(fixture.groupId(), details.groupId());
        assertEquals(fixture.semesterId(), details.startSemesterId());
        assertEquals(fixture.semesterId(), details.endSemesterId());
        assertEquals(fixture.seminarTeacherId(), details.teacherId());
        assertEquals(1, details.works().size());
        assertEquals(30, details.works().getFirst().scores().getFirst().score());
        assertEquals(75.0, details.students().getFirst().rating());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM course_works WHERE course_id = ?", Integer.class,
                fixture.seminarCourseId()));
    }

    @Test
    void lectureTeacherCannotReadSeminarResults() {
        Fixture fixture = insertFixture();

        assertThrows(ActorAccessDeniedException.class, () -> service.getForTeacher(
                fixture.seminarCourseId(), new TeacherID(fixture.lectureTeacherId())));
        assertEquals(CourseType.LECTURE, service.getForTeacher(
                fixture.lectureCourseId(), new TeacherID(fixture.lectureTeacherId())).courseType());
    }

    @Test
    void completionRequiresAtLeastOneWorkAndPersistsStatus() {
        Fixture fixture = insertFixture();
        TeacherID teacher = new TeacherID(fixture.seminarTeacherId());
        assertThrows(CourseWithoutWorksException.class, () -> service.complete(fixture.seminarCourseId(), teacher));

        service.addWork(fixture.seminarCourseId(), teacher,
                new CourseWorkRequest("Работа", 10, Date.valueOf("2026-10-10").toLocalDate()));
        service.complete(fixture.seminarCourseId(), teacher);

        assertEquals(CourseStatus.COMPLETED, service.getForTeacher(fixture.seminarCourseId(), teacher).status());
        long controlSheetId = controlSheets.createForCompletedCourse(
                fixture.seminarCourseId(), teacher, new GradingScale(85, 70, 50));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM control_sheets WHERE id = ? " +
                "AND control_form = 'EXAM' AND grade_5_from = 85", Integer.class, controlSheetId));
    }

    @Test
    void deanCanReopenCompletedCourseInNextSemesterAndExistingScoresRemain() {
        Fixture fixture = insertFixture();
        TeacherID teacher = new TeacherID(fixture.seminarTeacherId());
        long workNumber = service.addWork(fixture.seminarCourseId(), teacher,
                new CourseWorkRequest("Работа", 20, Date.valueOf("2026-10-10").toLocalDate()));
        service.enterScore(fixture.seminarCourseId(), workNumber, fixture.studentId(), teacher, 12);
        service.complete(fixture.seminarCourseId(), teacher);
        insert(
                "INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "API test next semester", Date.valueOf("2027-02-01"), Date.valueOf("2027-06-30"));

        assertTrue(service.canReopenInNextSemester(fixture.seminarCourseId()));
        service.reopenInNextSemester(fixture.seminarCourseId(), new DeanID(8));
        entityManager.flush();

        var reopened = service.getForTeacher(fixture.seminarCourseId(), teacher);
        assertEquals(CourseStatus.IN_PROGRESS, reopened.status());
        assertEquals("API test next semester", reopened.endSemesterName());
        assertEquals(12, reopened.works().getFirst().scores().getFirst().score());

        service.enterScore(fixture.seminarCourseId(), workNumber, fixture.studentId(), teacher, 18);
        assertEquals(18, service.getForTeacher(fixture.seminarCourseId(), teacher)
                .works().getFirst().scores().getFirst().score());
    }

    @Test
    void cannotReopenCourseInSemesterOtherThanTheNextOne() {
        Fixture fixture = insertFixture();
        TeacherID teacher = new TeacherID(fixture.seminarTeacherId());
        service.addWork(fixture.seminarCourseId(), teacher,
                new CourseWorkRequest("Работа", 20, Date.valueOf("2026-10-10").toLocalDate()));
        service.complete(fixture.seminarCourseId(), teacher);
        insert("INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "API test next semester", Date.valueOf("2027-02-01"), Date.valueOf("2027-06-30"));
        long laterSemesterId = insert(
                "INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "API test later semester", Date.valueOf("2027-09-01"), Date.valueOf("2028-01-31"));

        assertThrows(CatalogBadRequestException.class,
                () -> service.reopen(fixture.seminarCourseId(), new DeanID(8), laterSemesterId));
    }

    @Test
    void studentReadsOnlyTheirOwnScoresAndComputedRating() {
        Fixture fixture = insertFixture();
        long anotherStudentId = insert("INSERT INTO students (first_name, last_name, record_book_number, group_id) " +
                "VALUES ('Пётр', 'Иванов', 'API00002', ?) RETURNING id", fixture.groupId());
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)",
                fixture.seminarCourseId(), anotherStudentId);
        long workId = insert("INSERT INTO course_works (course_id, number, title, max_score, work_date) " +
                "VALUES (?, 1, 'Лабораторная', 20, ?) RETURNING id",
                fixture.seminarCourseId(), Date.valueOf("2026-10-01"));
        jdbc.update("INSERT INTO course_scores (work_id, student_id, score) VALUES (?, ?, ?)",
                workId, fixture.studentId(), 17);
        jdbc.update("INSERT INTO course_scores (work_id, student_id, score) VALUES (?, ?, ?)",
                workId, anotherStudentId, 7);

        var detail = studentCourses.get(new StudentID(fixture.studentId()), fixture.seminarCourseId());
        var summary = studentCourses.list(new StudentID(fixture.studentId())).getFirst();

        assertEquals(85.0, detail.rating());
        assertEquals(17, detail.works().getFirst().score());
        assertEquals(85.0, summary.rating());
        assertEquals(CourseType.SEMINAR, detail.courseType());
        assertEquals(2, studentCourses.list(new StudentID(fixture.studentId())).size());
        assertNull(studentCourses.get(new StudentID(fixture.studentId()), fixture.lectureCourseId()).rating());
        assertThrows(CourseNotFoundException.class,
                () -> studentCourses.get(new StudentID(anotherStudentId + 1000), fixture.seminarCourseId()));
    }

    @Test
    void studentCanReadControlResultAfterCourseIsReopened() {
        Fixture fixture = insertFixture();
        TeacherID teacher = new TeacherID(fixture.seminarTeacherId());
        long workNumber = service.addWork(fixture.seminarCourseId(), teacher,
                new CourseWorkRequest("Работа", 20, Date.valueOf("2026-10-10").toLocalDate()));
        service.enterScore(fixture.seminarCourseId(), workNumber, fixture.studentId(), teacher, 17);
        service.complete(fixture.seminarCourseId(), teacher);

        long sheetId = controlSheets.createForCompletedCourse(
                fixture.seminarCourseId(), teacher, new GradingScale(85, 70, 50));
        controlSheets.enterNumericResult(sheetId, teacher, new StudentID(fixture.studentId()), 88, null);
        controlSheets.close(sheetId, teacher);

        insert("INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "API test next semester", Date.valueOf("2027-02-01"), Date.valueOf("2027-06-30"));
        service.reopenInNextSemester(fixture.seminarCourseId(), new DeanID(1));

        var summary = studentCourses.list(new StudentID(fixture.studentId())).stream()
                .filter(course -> course.courseId() == fixture.seminarCourseId())
                .findFirst()
                .orElseThrow();
        var details = studentCourses.get(new StudentID(fixture.studentId()), fixture.seminarCourseId());

        assertEquals(CourseStatus.IN_PROGRESS, summary.status());
        assertEquals(85.0, summary.rating());
        assertEquals(ControlSheetStatus.CLOSED, summary.controlResult().sheetStatus());
        assertEquals(86.5, summary.controlResult().finalScore());
        assertEquals(5, summary.controlResult().finalGrade());
        assertEquals(5, details.controlResult().finalGrade());
    }

    @Test
    void assignsCourseworkToGroupAndPersistsCourseworkSheetResult() {
        Fixture fixture = insertFixture();
        var course = courseAssignment.assign(new CourseCreationRequest(
                fixture.disciplineId(), fixture.groupId(), fixture.semesterId(), fixture.semesterId(),
                CourseType.COURSEWORK, fixture.seminarTeacherId()
        ));
        var sheet = courseworkSheets.findByCourse_Id(course.id()).orElseThrow();

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM course_students WHERE course_id = ?",
                Integer.class, course.id()));

        courseworkApplication.enterScore(sheet.getId(), new TeacherID(fixture.seminarTeacherId()),
                new StudentID(fixture.studentId()), 84);
        var response = courseworkQueries.getForTeacher(sheet.getId(), new TeacherID(fixture.seminarTeacherId()));

        assertEquals(84, response.results().getFirst().score());
        assertEquals(CourseworkGrade.FOUR, response.results().getFirst().grade());
        var studentCourse = studentCourses.get(new StudentID(fixture.studentId()), course.id());
        assertEquals(84, studentCourse.courseworkScore());
        assertEquals(CourseworkGrade.FOUR, studentCourse.courseworkGrade());
        assertThrows(com.dev.csas.course.CourseNotCompletedException.class,
                () -> courseworkApplication.close(sheet.getId(), new TeacherID(fixture.seminarTeacherId())));
        service.complete(course.id(), new TeacherID(fixture.seminarTeacherId()));
        courseworkApplication.close(sheet.getId(), new TeacherID(fixture.seminarTeacherId()));
        entityManager.flush();
        assertEquals("CLOSED", jdbc.queryForObject("SELECT status FROM coursework_sheets WHERE id = ?",
                String.class, sheet.getId()));

        long nextSemesterId = insert(
                "INSERT INTO semesters (name, start_date, end_date) VALUES (?, ?, ?) RETURNING id",
                "API test next semester", Date.valueOf("2027-02-01"), Date.valueOf("2027-06-30"));
        courseworkApplication.reopen(sheet.getId(), new DeanID(1), nextSemesterId);
        entityManager.flush();
        assertEquals("OPEN", jdbc.queryForObject("SELECT status FROM coursework_sheets WHERE id = ?",
                String.class, sheet.getId()));
    }

    @Test
    void updatesCourseGroupWhileItsAutomaticallyCreatedSheetIsEmpty() {
        Fixture fixture = insertFixture();
        long otherGroupId = insert("INSERT INTO groups " +
                        "(name, year_of_admission, specialty, course, education_form, study_form) " +
                        "VALUES ('API-TEST-OTHER', 2024, 'Информатика', 3, 'BUDGET', 'FULL_TIME') RETURNING id");
        long otherStudentId = insert("INSERT INTO students " +
                        "(first_name, last_name, record_book_number, group_id) " +
                        "VALUES ('Мария', 'Смирнова', 'API00003', ?) RETURNING id", otherGroupId);
        var assigned = courseAssignment.assign(new CourseCreationRequest(
                fixture.disciplineId(), fixture.groupId(), fixture.semesterId(), fixture.semesterId(),
                CourseType.COURSEWORK, fixture.seminarTeacherId()
        ));

        courseAssignment.update(assigned.id(), new CourseCreationRequest(
                fixture.disciplineId(), otherGroupId, fixture.semesterId(), fixture.semesterId(),
                CourseType.COURSEWORK, fixture.seminarTeacherId()
        ));
        entityManager.flush();

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM course_students " +
                "WHERE course_id = ? AND student_id = ?", Integer.class, assigned.id(), otherStudentId));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM course_students " +
                "WHERE course_id = ? AND student_id = ?", Integer.class, assigned.id(), fixture.studentId()));
        assertTrue(courseworkSheets.findByCourse_Id(assigned.id()).isPresent());
    }

    @Test
    void deletesCourseWithOnlyAnEmptyAutomaticallyCreatedSheet() {
        Fixture fixture = insertFixture();
        var assigned = courseAssignment.assign(new CourseCreationRequest(
                fixture.disciplineId(), fixture.groupId(), fixture.semesterId(), fixture.semesterId(),
                CourseType.COURSEWORK, fixture.seminarTeacherId()
        ));

        courseAssignment.delete(assigned.id());
        entityManager.flush();

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE id = ?",
                Integer.class, assigned.id()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM coursework_sheets WHERE course_id = ?",
                Integer.class, assigned.id()));
    }

    @Test
    void createsAndDeletesStudentAccountWithoutDeletingItsProfile() {
        Fixture fixture = insertFixture();
        doReturn(Map.of()).when(sessions).findByPrincipalName("api-student");

        var account = userAdministration.create(new UserCreationRequest(
                "api-student", "password123", RoleEnum.STUDENT, fixture.studentId()
        ));

        var persistedAccount = users.findById(account.id()).orElseThrow();
        assertTrue(passwordEncoder.matches("password123", persistedAccount.getPassword()));
        assertEquals(fixture.studentId(), account.profileId());
        assertEquals(account.id(), students.findByUser_Id(account.id()).orElseThrow().getUser().getId());

        userAdministration.delete(account.id(), "admin");
        entityManager.flush();

        assertTrue(users.findById(account.id()).isEmpty());
        assertNull(students.findById(fixture.studentId()).orElseThrow().getUser());
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
        long seminarCourseId = insert("INSERT INTO courses " +
                        "(discipline_id, group_id, start_semester_id, end_semester_id, course_type, teacher_id, status) " +
                        "VALUES (?, ?, ?, ?, 'SEMINAR', ?, 'IN_PROGRESS') RETURNING id",
                disciplineId, groupId, semesterId, semesterId, seminarTeacherId);
        long lectureCourseId = insert("INSERT INTO courses " +
                        "(discipline_id, group_id, start_semester_id, end_semester_id, course_type, teacher_id, status) " +
                        "VALUES (?, ?, ?, ?, 'LECTURE', ?, 'IN_PROGRESS') RETURNING id",
                disciplineId, groupId, semesterId, semesterId, lectureTeacherId);
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)", seminarCourseId, studentId);
        jdbc.update("INSERT INTO course_students (course_id, student_id) VALUES (?, ?)", lectureCourseId, studentId);
        return new Fixture(seminarCourseId, lectureCourseId, studentId,
                seminarTeacherId, lectureTeacherId, groupId, disciplineId, semesterId);
    }

    private long insert(String sql, Object... parameters) {
        Long id = jdbc.queryForObject(sql, Long.class, parameters);
        assertNotNull(id);
        return id;
    }

    private record Fixture(long seminarCourseId, long lectureCourseId, long studentId,
                           long seminarTeacherId, long lectureTeacherId, long groupId,
                           long disciplineId, long semesterId) {
    }
}
