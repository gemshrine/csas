package com.dev.csas.api;

import com.dev.csas.api.attendance.AttendanceSheetController;
import com.dev.csas.api.control.ControlSheetController;
import com.dev.csas.api.course.CourseListController;
import com.dev.csas.api.course.CourseController;
import com.dev.csas.api.course.CourseCreationRequest;
import com.dev.csas.api.course.CourseDetailsResponse;
import com.dev.csas.api.course.StudentCourseController;
import com.dev.csas.api.course.StudentCourseDetailsResponse;
import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.api.attendance.AttendanceSheetDetailsResponse;
import com.dev.csas.attendance.AttendanceSheetStatus;
import com.dev.csas.control.PassFail;
import com.dev.csas.api.error.ApiExceptionHandler;
import com.dev.csas.attendance.AttendanceSheetApplicationService;
import com.dev.csas.attendance.AttendanceSheetNotFoundException;
import com.dev.csas.control.ControlSheetApplicationService;
import com.dev.csas.control.ControlSheetQueryService;
import com.dev.csas.attendance.AttendanceSheetQueryService;
import com.dev.csas.course.CourseQueryService;
import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseAssignmentService;
import com.dev.csas.course.StudentCourseQueryService;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.LectureID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AttendanceSheetController.class, ControlSheetController.class,
        CourseListController.class, CourseController.class, StudentCourseController.class})
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
class SheetApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AttendanceSheetApplicationService attendance;

    @MockitoBean
    private ControlSheetApplicationService controls;

    @MockitoBean
    private ControlSheetQueryService controlQueries;

    @MockitoBean
    private AttendanceSheetQueryService attendanceQueries;

    @MockitoBean
    private CourseQueryService courseQueries;

    @MockitoBean
    private CourseAssignmentService courseAssignment;

    @MockitoBean
    private CourseApplicationService courseApplication;

    @MockitoBean
    private StudentCourseQueryService studentCourseQueries;

    @MockitoBean
    private CurrentActorResolver actors;

    @Test
    void rejectsUnauthenticatedApiRequestWithJson() throws Exception {
        mvc.perform(get("/api/attendance-sheets/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Требуется аутентификация"));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void getsControlSheetUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));
        when(controlQueries.getForTeacher(3, new TeacherID(12)))
                .thenReturn(new ControlSheetDetailsResponse(3, null, null, null, null, java.util.List.of()));

        mvc.perform(get("/api/control-sheets/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.students").isArray());

        verify(controlQueries).getForTeacher(3, new TeacherID(12));
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void deniesStudentFromReadingControlSheet() throws Exception {
        mvc.perform(get("/api/control-sheets/3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void listsCoursesForCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));
        when(courseQueries.listForTeacher(new TeacherID(12))).thenReturn(java.util.List.of());

        mvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(courseQueries).listForTeacher(new TeacherID(12));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deanCanAssignCourse() throws Exception {
        when(actors.dean(any())).thenReturn(new DeanID(8));
        when(courseAssignment.assign(any(CourseCreationRequest.class))).thenReturn(
                new CourseHeaderResponse(15, "Базы данных", "ИВТ-301", "Осень 2026", "Весна 2027",
                        com.dev.csas.course.CourseType.COURSEWORK, com.dev.csas.course.CourseStatus.IN_PROGRESS));

        mvc.perform(post("/api/courses").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"disciplineId":4,"groupId":2,"startSemesterId":3,"endSemesterId":4,
                                 "courseType":"COURSEWORK","teacherId":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseType").value("COURSEWORK"))
                .andExpect(jsonPath("$.startSemesterName").value("Осень 2026"));

        verify(courseAssignment).assign(any(CourseCreationRequest.class));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void seminarTeacherCanCreateControlSheetWithGradingScale() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));
        when(controls.createForCompletedCourse(eq(3L), eq(new TeacherID(12)), any()))
                .thenReturn(34L);

        mvc.perform(post("/api/courses/3/control-sheet").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade5From\":85,\"grade4From\":70,\"grade3From\":50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sheetId").value(34));

        verify(controls).createForCompletedCourse(eq(3L), eq(new TeacherID(12)), any());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void rejectsUnorderedGradingScale() throws Exception {
        mvc.perform(post("/api/courses/3/control-sheet").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade5From\":70,\"grade4From\":80,\"grade3From\":50}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void getsCourseDetailsUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));
        when(courseApplication.getForTeacher(eq(3L), eq(new TeacherID(12))))
                .thenReturn(new CourseDetailsResponse(3, "Базы данных", "ИВТ-301", "Осень 2026", "Весна 2027",
                        com.dev.csas.course.CourseType.SEMINAR, com.dev.csas.course.CourseStatus.IN_PROGRESS,
                        java.util.List.of(), java.util.List.of()));

        mvc.perform(get("/api/courses/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(3))
                .andExpect(jsonPath("$.works").isArray())
                .andExpect(jsonPath("$.students").isArray());

        verify(courseApplication).getForTeacher(3, new TeacherID(12));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deniesDeanFromChangingCourseWorks() throws Exception {
        mvc.perform(post("/api/courses/3/works").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Работа\",\"maxScore\":20,\"date\":\"2026-10-01\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void getsOwnCoursePerformanceUsingCurrentStudent() throws Exception {
        when(actors.student(any())).thenReturn(new StudentID(21));
        when(studentCourseQueries.get(new StudentID(21), 3L)).thenReturn(
                new StudentCourseDetailsResponse(3, "Базы данных", "ИВТ-301", "Осень 2026", "Весна 2027",
                        com.dev.csas.course.CourseType.SEMINAR, com.dev.csas.course.CourseStatus.IN_PROGRESS, 90.0,
                        null, null,
                        new com.dev.csas.api.course.StudentControlResultResponse(
                                com.dev.csas.discipline.ControlFormEnum.EXAM,
                                com.dev.csas.control.ControlSheetStatus.CLOSED,
                                88,
                                null,
                                89.0,
                                5
                        ),
                        java.util.List.of(new StudentCourseDetailsResponse.WorkResult(1, "Лабораторная", 20,
                                java.time.LocalDate.parse("2026-10-01"), 18))));

        mvc.perform(get("/api/me/courses/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(90.0))
                .andExpect(jsonPath("$.controlResult.numericScore").value(88))
                .andExpect(jsonPath("$.controlResult.finalScore").value(89.0))
                .andExpect(jsonPath("$.controlResult.finalGrade").value(5))
                .andExpect(jsonPath("$.works[0].score").value(18))
                .andExpect(jsonPath("$.studentId").doesNotExist());

        verify(studentCourseQueries).get(new StudentID(21), 3L);
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void deniesTeacherFromStudentPersonalPerformanceApi() throws Exception {
        mvc.perform(get("/api/me/courses"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void getsAttendanceSheetUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));
        when(attendanceQueries.getForTeacher(4, new TeacherID(12)))
                .thenReturn(new AttendanceSheetDetailsResponse(4, null, AttendanceSheetStatus.OPEN,
                        java.util.List.of(), java.util.List.of()));

        mvc.perform(get("/api/attendance-sheets/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.students").isArray())
                .andExpect(jsonPath("$.lectures").isArray());

        verify(attendanceQueries).getForTeacher(4, new TeacherID(12));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void createsLectureAndReturnsGeneratedId() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));
        when(attendance.addLecture(eq(3L), eq(new TeacherID(12)), eq(java.time.LocalDate.parse("2026-10-05")), eq("Введение")))
                .thenReturn(new LectureID(51));

        mvc.perform(post("/api/attendance-sheets/3/lectures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-05\",\"title\":\"Введение\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(51))
                .andExpect(jsonPath("$.date").value("2026-10-05"))
                .andExpect(jsonPath("$.title").value("Введение"));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deniesDeanFromAddingLecture() throws Exception {
        mvc.perform(post("/api/attendance-sheets/3/lectures").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-05\",\"title\":\"Введение\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void rejectsInvalidNumericResultBeforeCallingService() throws Exception {
        mvc.perform(put("/api/control-sheets/3/results/8/numeric").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void entersNumericResultUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));

        mvc.perform(put("/api/control-sheets/3/results/8/numeric").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":88,\"reason\":\"Пересдача\"}"))
                .andExpect(status().isNoContent());

        verify(controls).enterNumericResult(3, new TeacherID(12), new StudentID(8), 88, "Пересдача");
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void entersPassFailResultUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));

        mvc.perform(put("/api/control-sheets/3/results/8/pass-fail").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PASS\"}"))
                .andExpect(status().isNoContent());

        verify(controls).enterPassFailResult(3, new TeacherID(12), new StudentID(8), PassFail.PASS, null);
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void registersAttendanceUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));

        mvc.perform(put("/api/attendance-sheets/3/lectures/5/attendance/8").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ABSENT\"}"))
                .andExpect(status().isNoContent());

        verify(attendance).registerAttendance(
                3,
                new TeacherID(12),
                new StudentID(8),
                com.dev.csas.attendance.AttendanceStatusEnum.ABSENT,
                new LectureID(5)
        );
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void archivesAttendanceSheetUsingCurrentDean() throws Exception {
        when(actors.dean(any())).thenReturn(new DeanID(7));

        mvc.perform(post("/api/attendance-sheets/3/archive").with(csrf()))
                .andExpect(status().isNoContent());

        verify(attendance).archive(3, new DeanID(7));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void reopensControlSheetWithReasonUsingCurrentDean() throws Exception {
        when(actors.dean(any())).thenReturn(new DeanID(7));

        mvc.perform(post("/api/control-sheets/3/reopen").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Исправление результата\"}"))
                .andExpect(status().isNoContent());

        verify(controls).reopen(3, new DeanID(7), "Исправление результата");
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void mapsMissingAttendanceSheetToNotFound() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));
        doThrow(new AttendanceSheetNotFoundException("Ведомость посещаемости не найдена: 3"))
                .when(attendance).close(3, new TeacherID(12));

        mvc.perform(post("/api/attendance-sheets/3/close").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ведомость посещаемости не найдена: 3"));
    }
}
