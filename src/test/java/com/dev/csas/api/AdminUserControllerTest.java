package com.dev.csas.api;

import com.dev.csas.api.error.ApiExceptionHandler;
import com.dev.csas.api.user.AdminUserController;
import com.dev.csas.api.user.UserAccountResponse;
import com.dev.csas.api.user.UserProfileOptionsResponse;
import com.dev.csas.security.SecurityConfiguration;
import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserAdministrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserAdministrationService users;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanListAccountsWithoutPasswordData() throws Exception {
        when(users.findAll()).thenReturn(List.of(
                new UserAccountResponse(3L, "student", RoleEnum.STUDENT, true, 5L, "Иванов И.И.")
        ));

        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].username").value("student"))
                .andExpect(jsonPath("$[0].role").value("STUDENT"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].profileId").value(5))
                .andExpect(jsonPath("$[0].profileName").value("Иванов И.И."))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        verify(users).findAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanListUnlinkedTeacherAndStudentProfiles() throws Exception {
        when(users.findAvailableProfiles()).thenReturn(new UserProfileOptionsResponse(
                List.of(new UserProfileOptionsResponse.ProfileOption(7L, "Петров Пётр")),
                List.of(new UserProfileOptionsResponse.ProfileOption(15L, "TST0000015 Иванов Иван"))
        ));

        mvc.perform(get("/api/admin/users/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teachers[0].id").value(7))
                .andExpect(jsonPath("$.students[0].name").value("TST0000015 Иванов Иван"));

        verify(users).findAvailableProfiles();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanChangeAccountRole() throws Exception {
        when(users.changeRole(3L, RoleEnum.TEACHER, null))
                .thenReturn(new UserAccountResponse(3L, "teacher", RoleEnum.TEACHER, true));

        mvc.perform(put("/api/admin/users/3/role")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));

        verify(users).changeRole(3L, RoleEnum.TEACHER, null);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateAccount() throws Exception {
        when(users.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UserAccountResponse(9L, "dean", RoleEnum.DEAN, true));

        mvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"username\":\"dean\",\"password\":\"password123\",\"role\":\"DEAN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.username").value("dean"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanUpdateAccountStatus() throws Exception {
        when(users.update(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("admin")))
                .thenReturn(new UserAccountResponse(9L, "dean", RoleEnum.DEAN, false));

        mvc.perform(patch("/api/admin/users/9")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanDeleteAccount() throws Exception {
        mvc.perform(delete("/api/admin/users/9").with(csrf()))
                .andExpect(status().isNoContent());

        verify(users).delete(9L, "admin");
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deniesNonAdminFromAccountManagement() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
