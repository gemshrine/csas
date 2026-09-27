package com.dev.csas.web;

import com.dev.csas.attendance.AttendanceSheetQueryService;
import com.dev.csas.control.ControlSheetQueryService;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
}
