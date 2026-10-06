package com.dev.csas.api;

import com.dev.csas.api.course.CourseController;
import com.dev.csas.control.ControlSheetApplicationService;
import com.dev.csas.course.CourseAssignmentService;
import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.api.error.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
class CourseManagementControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CourseApplicationService courses;
    @MockitoBean
    private CourseAssignmentService assignments;
    @MockitoBean
    private ControlSheetApplicationService controlSheets;
    @MockitoBean
    private CurrentActorResolver actors;

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deanCanUpdateCourseAssignment() throws Exception {
        when(assignments.update(eq(8L), any())).thenReturn(new CourseHeaderResponse(
                8L, "Архитектура ЭВМ", "ИВТ-22", "Семестр 3", "Семестр 4",
                CourseType.SEMINAR, CourseStatus.IN_PROGRESS
        ));

        mvc.perform(put("/api/courses/8")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"disciplineId\":3,\"groupId\":4,\"startSemesterId\":5," +
                                "\"endSemesterId\":6,\"courseType\":\"SEMINAR\",\"teacherId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.teacherId").doesNotExist());

        verify(actors).dean(any());
        verify(assignments).update(eq(8L), any());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deanCanDeleteCourse() throws Exception {
        mvc.perform(delete("/api/courses/8").with(csrf()))
                .andExpect(status().isNoContent());

        verify(actors).dean(any());
        verify(assignments).delete(8L);
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void teacherCannotChangeCourseAssignment() throws Exception {
        mvc.perform(delete("/api/courses/8").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
