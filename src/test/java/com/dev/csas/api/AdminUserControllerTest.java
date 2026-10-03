package com.dev.csas.api;

import com.dev.csas.api.error.ApiExceptionHandler;
import com.dev.csas.api.user.AdminUserController;
import com.dev.csas.api.user.UserAccountResponse;
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
                new UserAccountResponse(3L, "student", RoleEnum.STUDENT, true)
        ));

        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].username").value("student"))
                .andExpect(jsonPath("$[0].role").value("STUDENT"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        verify(users).findAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanChangeAccountRole() throws Exception {
        when(users.changeRole(3L, RoleEnum.TEACHER))
                .thenReturn(new UserAccountResponse(3L, "teacher", RoleEnum.TEACHER, true));

        mvc.perform(put("/api/admin/users/3/role")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));

        verify(users).changeRole(3L, RoleEnum.TEACHER);
    }

    @Test
    @WithMockUser(username = "dean", roles = "DEAN")
    void deniesNonAdminFromAccountManagement() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
