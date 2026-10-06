package com.dev.csas.api;

import com.dev.csas.api.error.ApiExceptionHandler;
import com.dev.csas.api.user.AdminUserActionController;
import com.dev.csas.audit.UserActionResponse;
import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserActionController.class)
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
class AdminUserActionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserActionService userActions;

    @Test
    void adminCanViewRecentActions() throws Exception {
        when(userActions.findRecent()).thenReturn(List.of(new UserActionResponse(
                5L,
                "admin",
                "ADMIN",
                UserActionType.GROUP_CREATED,
                "GROUP",
                22L,
                null,
                Instant.parse("2026-10-06T10:00:00Z")
        )));

        mvc.perform(get("/api/admin/audit-events").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].actorUsername").value("admin"))
                .andExpect(jsonPath("$[0].actorRole").value("ADMIN"))
                .andExpect(jsonPath("$[0].actionType").value("GROUP_CREATED"))
                .andExpect(jsonPath("$[0].entityType").value("GROUP"))
                .andExpect(jsonPath("$[0].entityId").value(22));

        verify(userActions).findRecent();
    }

    @Test
    void deniesNonAdminFromActionJournal() throws Exception {
        mvc.perform(get("/api/admin/audit-events").with(user("dean").roles("DEAN")))
                .andExpect(status().isForbidden());
    }
}
