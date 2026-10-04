package com.dev.csas.api;

import com.dev.csas.api.error.ApiExceptionHandler;
import com.dev.csas.api.report.ReportController;
import com.dev.csas.report.ReportExportService;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReportExportService reports;

    @MockitoBean
    private CurrentActorResolver actors;

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void downloadsCourseXlsxForCurrentTeacher() throws Exception {
        byte[] workbook = {80, 75, 3, 4};
        when(actors.teacher(any())).thenReturn(new TeacherID(7));
        when(reports.course(12, new TeacherID(7), false)).thenReturn(workbook);

        mvc.perform(get("/api/reports/courses/12/xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("course-12.xlsx")))
                .andExpect(content().bytes(workbook));

        verify(reports).course(12, new TeacherID(7), false);
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void studentCannotDownloadAnotherStudentsAcademicRecord() throws Exception {
        when(actors.student(any())).thenReturn(new StudentID(21));

        mvc.perform(get("/api/reports/students/22/academic-record.xlsx"))
                .andExpect(status().isForbidden());

        verify(reports, never()).studentAcademicRecord(anyLong());
    }

    @Test
    void rejectsUnauthenticatedReportRequest() throws Exception {
        mvc.perform(get("/api/reports/statistics.xlsx"))
                .andExpect(status().isUnauthorized());
    }
}
