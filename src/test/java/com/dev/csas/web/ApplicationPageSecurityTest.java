package com.dev.csas.web;

import com.dev.csas.api.course.CourseDetailsResponse;
import com.dev.csas.api.course.StudentCourseDetailsResponse;
import com.dev.csas.backup.DatabaseBackupException;
import com.dev.csas.backup.DatabaseBackupService;
import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;
import com.dev.csas.course.StudentCourseQueryService;
import com.dev.csas.discipline.DisciplineService;
import com.dev.csas.group.GroupService;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.semester.SemesterService;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentService;
import com.dev.csas.teacher.TeacherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ApplicationPageController.class, CatalogPageController.class, AdminPageController.class,
        ReportPageController.class, DatabaseBackupController.class})
@Import(SecurityConfiguration.class)
class ApplicationPageSecurityTest {

    @Autowired
    private MockMvc mvc;

    @TempDir
    private Path temporaryDirectory;

    @MockitoBean
    private CourseApplicationService courseDetails;

    @MockitoBean
    private StudentCourseQueryService studentCourses;

    @MockitoBean
    private CurrentActorResolver actors;

    @MockitoBean
    private GroupService groups;

    @MockitoBean
    private StudentService students;

    @MockitoBean
    private TeacherService teachers;

    @MockitoBean
    private DisciplineService disciplines;

    @MockitoBean
    private SemesterService semesters;

    @MockitoBean
    private DatabaseBackupService backups;

    @Test
    void redirectsUnauthenticatedPageRequestToLoginAndKeepsReturnUrl() throws Exception {
        mvc.perform(get("/courses").queryParam("semester", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?returnUrl=%2Fcourses%3Fsemester%3D1"));
    }

    @Test
    void servesApiClientScriptToUnauthenticatedUsers() throws Exception {
        mvc.perform(get("/api-client.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("window.CsasApi")));
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void deniesStudentFromTeacherCourseList() throws Exception {
        mvc.perform(get("/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void deniesTeacherFromCatalogPages() throws Exception {
        mvc.perform(get("/groups"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void deniesTeacherFromStudentCoursePages() throws Exception {
        mvc.perform(get("/me/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void deniesTeacherFromCourseCreationPage() throws Exception {
        mvc.perform(get("/courses/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deanCanOpenCourseCreationPage() throws Exception {
        mvc.perform(get("/courses/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Создать курс")));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deanCanOpenCatalogPage() throws Exception {
        when(groups.findAll()).thenReturn(List.of());

        mvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Справочник")));
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void studentCanOpenOwnCoursesPage() throws Exception {
        mvc.perform(get("/me/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Моя успеваемость")));
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void studentCanOpenOwnCourseDetailsPage() throws Exception {
        when(actors.student(any(Authentication.class))).thenReturn(new StudentID(7));
        when(studentCourses.get(new StudentID(7), 3)).thenReturn(new StudentCourseDetailsResponse(
                3, "Дисциплина", "Группа", "Семестр 1", "Семестр 2", CourseType.SEMINAR,
                CourseStatus.IN_PROGRESS, 90.0, null, null, null, List.of()
        ));

        mvc.perform(get("/me/courses/3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-course-id=\"3\"")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanOpenAccountManagementPage() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Учётные записи")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("account-create-form")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("account-edit-dialog")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanOpenActionJournalPage() throws Exception {
        mvc.perform(get("/admin/audit-events"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Журнал действий")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanOpenDatabaseBackupPage() throws Exception {
        mvc.perform(get("/admin/backups"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Создать и скачать копию")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deniesDeanFromDatabaseBackupPage() throws Exception {
        mvc.perform(get("/admin/backups"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void redirectsToBackupPageWhenDatabaseBackupFails() throws Exception {
        org.mockito.Mockito.doThrow(new DatabaseBackupException("failed", new IllegalStateException()))
                .when(backups).createBackup();

        mvc.perform(post("/admin/backups").with(csrf()))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/admin/backups?error=creation"));

        mvc.perform(get("/admin/backups").queryParam("error", "creation"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Не удалось создать резервную копию")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void downloadsBackupArchiveFromMvcEndpoint() throws Exception {
        byte[] archive = {4, 3, 2, 1};
        Path backup = temporaryDirectory.resolve("backup.dump");
        Files.write(backup, archive);
        org.mockito.Mockito.when(backups.createBackup()).thenReturn(backup);

        MvcResult result = mvc.perform(post("/admin/backups").with(csrf()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
                .andExpect(content().bytes(archive));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deniesDeanFromActionJournalPage() throws Exception {
        mvc.perform(get("/admin/audit-events"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deniesDeanFromAccountManagementPage() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deanCanOpenReportsPage() throws Exception {
        mvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Скачать список должников XLSX")));
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void deniesStudentFromReportsPage() throws Exception {
        mvc.perform(get("/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deanCanReopenCourseFromBrowser() throws Exception {
        when(actors.dean(any(Authentication.class))).thenReturn(new DeanID(8));

        mvc.perform(post("/courses/3/reopen").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/3"));

        verify(courseDetails).reopenInNextSemester(3, new DeanID(8));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void completedCoursePageShowsReopenActionWhenNextSemesterExists() throws Exception {
        when(actors.dean(any(Authentication.class))).thenReturn(new DeanID(8));
        when(courseDetails.getForDean(3)).thenReturn(new CourseDetailsResponse(
                3, "Дисциплина", "Группа", "Семестр 1", "Семестр 2", CourseType.SEMINAR,
                CourseStatus.COMPLETED, List.of(), List.of()
        ));
        when(courseDetails.canReopenInNextSemester(3)).thenReturn(true);

        mvc.perform(get("/courses/3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Переоткрыть в следующем семестре"
                )))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("course-management")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Изменить курс")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Удалить курс")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("scores-readonly")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("id=\"course-management\" hidden")
                )));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void deniesTeacherFromReopeningCourseFromBrowser() throws Exception {
        mvc.perform(post("/courses/3/reopen").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void browserLogoutRedirectsToLogin() throws Exception {
        mvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void browserLogoutRequiresCsrfToken() throws Exception {
        mvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }
}
