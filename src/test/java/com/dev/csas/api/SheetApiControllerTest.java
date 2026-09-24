package com.dev.csas.api;

import com.dev.csas.api.attendance.AttendanceSheetController;
import com.dev.csas.api.control.ControlSheetController;
import com.dev.csas.api.course.CourseListController;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AttendanceSheetController.class, ControlSheetController.class, CourseListController.class})
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
    private CurrentActorResolver actors;

    @Test
    void rejectsUnauthenticatedApiRequestWithJson() throws Exception {
        mvc.perform(get("/api/attendance-sheets/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("Basic")))
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

        mvc.perform(post("/api/attendance-sheets/3/lectures")
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
        mvc.perform(post("/api/attendance-sheets/3/lectures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-05\",\"title\":\"Введение\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void rejectsInvalidNumericResultBeforeCallingService() throws Exception {
        mvc.perform(put("/api/control-sheets/3/results/8/numeric")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void entersNumericResultUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));

        mvc.perform(put("/api/control-sheets/3/results/8/numeric")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":88,\"reason\":\"Пересдача\"}"))
                .andExpect(status().isNoContent());

        verify(controls).enterNumericResult(3, new TeacherID(12), new StudentID(8), 88, "Пересдача");
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void entersPassFailResultUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));

        mvc.perform(put("/api/control-sheets/3/results/8/pass-fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"PASS\"}"))
                .andExpect(status().isNoContent());

        verify(controls).enterPassFailResult(3, new TeacherID(12), new StudentID(8), PassFail.PASS, null);
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void registersAttendanceUsingCurrentTeacher() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(12));

        mvc.perform(put("/api/attendance-sheets/3/lectures/5/attendance/8")
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

        mvc.perform(post("/api/attendance-sheets/3/archive"))
                .andExpect(status().isNoContent());

        verify(attendance).archive(3, new DeanID(7));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void reopensControlSheetWithReasonUsingCurrentDean() throws Exception {
        when(actors.dean(any())).thenReturn(new DeanID(7));

        mvc.perform(post("/api/control-sheets/3/reopen")
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

        mvc.perform(post("/api/attendance-sheets/3/close"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ведомость посещаемости не найдена: 3"));
    }
}
