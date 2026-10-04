package com.dev.csas.web;

import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseQueryService;
import com.dev.csas.course.StudentCourseQueryService;
import com.dev.csas.discipline.DisciplineService;
import com.dev.csas.group.GroupService;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.semester.SemesterService;
import com.dev.csas.student.StudentService;
import com.dev.csas.teacher.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest({ApplicationPageController.class, CatalogPageController.class})
@Import(SecurityConfiguration.class)
class ApplicationPageSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CourseQueryService courses;

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

    @Test
    void redirectsUnauthenticatedPageRequestToLoginAndKeepsReturnUrl() throws Exception {
        mvc.perform(get("/courses").queryParam("semester", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?returnUrl=%2Fcourses%3Fsemester%3D1"));
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
