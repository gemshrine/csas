package com.dev.csas.group;

import com.dev.csas.api.error.ApiExceptionHandler;
import com.dev.csas.discipline.DisciplineController;
import com.dev.csas.discipline.DisciplineService;
import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.enums.StudyFormEnum;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.semester.SemesterController;
import com.dev.csas.semester.SemesterService;
import com.dev.csas.student.StudentController;
import com.dev.csas.student.StudentService;
import com.dev.csas.teacher.TeacherController;
import com.dev.csas.teacher.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        GroupController.class,
        StudentController.class,
        TeacherController.class,
        DisciplineController.class,
        SemesterController.class
})
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
class CatalogApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GroupService groupService;
    @MockitoBean
    private StudentService studentService;
    @MockitoBean
    private TeacherService teacherService;
    @MockitoBean
    private DisciplineService disciplineService;
    @MockitoBean
    private SemesterService semesterService;

    @Test
    void rejectsUnauthenticatedCatalogRequest() throws Exception {
        mvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Требуется аутентификация"))
                .andExpect(jsonPath("$.path").value("/api/students"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(username = "student", roles = "STUDENT")
    void forbidsStudentAccessToCatalog() throws Exception {
        mvc.perform(get("/api/students"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Недостаточно прав для выполнения операции"))
                .andExpect(jsonPath("$.path").value("/api/students"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void forbidsTeacherAccessToCatalog() throws Exception {
        mvc.perform(get("/api/groups"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/groups"));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void allowsDeanToReadCatalog() throws Exception {
        when(groupService.findAll()).thenReturn(List.of(new GroupResponse(
                1L,
                "ИВТ-41-1",
                2022,
                "Информатика",
                4,
                EducationFormEnum.BUDGET,
                StudyFormEnum.FULL_TIME
        )));

        mvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ИВТ-41-1"));
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void returnsNotFoundInSharedErrorFormat() throws Exception {
        when(groupService.findById(7L)).thenThrow(CatalogNotFoundException.group(7L));

        mvc.perform(get("/api/groups/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Группа с id 7 не найдена"))
                .andExpect(jsonPath("$.path").value("/api/groups/7"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void returnsValidationErrorInSharedErrorFormat() throws Exception {
        mvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value("/api/groups"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void returnsConflictWhenStudentIsEnrolledOnCourse() throws Exception {
        doThrow(new CatalogConflictException("Нельзя удалить студента: он зачислен на курс"))
                .when(studentService).delete(eq(1L));

        mvc.perform(delete("/api/students/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Нельзя удалить студента: он зачислен на курс"))
                .andExpect(jsonPath("$.path").value("/api/students/1"))
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void allowsDeanToCreateGroup() throws Exception {
        when(groupService.create(any())).thenReturn(new GroupResponse(
                1L,
                "ИВТ-41-1",
                2022,
                "Информатика",
                4,
                EducationFormEnum.BUDGET,
                StudyFormEnum.FULL_TIME
        ));

        mvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ИВТ-41-1",
                                  "yearOfAdmission": 2022,
                                  "specialty": "Информатика",
                                  "course": 4,
                                  "educationForm": "BUDGET",
                                  "studyForm": "FULL_TIME"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }
}
