package com.dev.csas.web;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.attendance.AttendanceSheetQueryService;
import com.dev.csas.control.ControlSheetQueryService;
import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;
import com.dev.csas.coursework.CourseworkSheetQueryService;
import com.dev.csas.coursework.CourseworkSheetDetailsResponse;
import com.dev.csas.coursework.CourseworkSheetStatus;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SheetPageController.class)
@Import(SecurityConfiguration.class)
class SheetPageSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ControlSheetQueryService controlSheets;

    @MockitoBean
    private AttendanceSheetQueryService attendanceSheets;

    @MockitoBean
    private CourseworkSheetQueryService courseworkSheets;

    @MockitoBean
    private CurrentActorResolver actors;

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void deniesStudentFromControlSheetPage() throws Exception {
        mvc.perform(get("/control-sheets/4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void deniesStudentFromAttendanceSheetPage() throws Exception {
        mvc.perform(get("/attendance-sheets/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void deniesStudentFromCourseworkSheetPage() throws Exception {
        mvc.perform(get("/coursework-sheets/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    void redirectsUnauthenticatedCourseworkSheetRequestToLogin() throws Exception {
        mvc.perform(get("/coursework-sheets/6"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void teacherCanOpenCourseworkSheetPage() throws Exception {
        when(actors.teacher(any())).thenReturn(new TeacherID(4));
        when(courseworkSheets.getForTeacher(6, new TeacherID(4))).thenReturn(new CourseworkSheetDetailsResponse(
                6,
                new CourseHeaderResponse(3, "Дисциплина", "Группа", "Семестр 1", "Семестр 1",
                        CourseType.COURSEWORK, CourseStatus.IN_PROGRESS),
                CourseworkSheetStatus.OPEN,
                List.of()
        ));

        mvc.perform(get("/coursework-sheets/6"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Ведомость курсовых работ")));
    }
}
