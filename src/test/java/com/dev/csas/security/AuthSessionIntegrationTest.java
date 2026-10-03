package com.dev.csas.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ActiveProfiles("dev")
@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=true")
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("csas_auth_test")
            .withUsername("csas")
            .withPassword("csas");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&currentSchema=csas_dev");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void developmentMigrationSeedsLinkedTestAccountsAndCourseSheets() {
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username LIKE 'demo-%'", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM teachers t JOIN users u ON u.id = t.user_id " +
                "WHERE u.username LIKE 'demo-%'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM students s JOIN users u ON u.id = s.user_id " +
                "WHERE u.username = 'demo-student'", Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM control_sheets sheet
                JOIN courses course ON course.id = sheet.course_id
                JOIN disciplines discipline ON discipline.id = course.discipline_id
                WHERE discipline.name = 'Демонстрационная дисциплина'
                  AND course.course_type = 'SEMINAR'
                """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM attendance_sheets sheet
                JOIN courses course ON course.id = sheet.course_id
                JOIN disciplines discipline ON discipline.id = course.discipline_id
                WHERE discipline.name = 'Демонстрационная дисциплина'
                  AND course.course_type = 'LECTURE'
                """, Integer.class));
    }

    @Test
    void loginCreatesFiveMinuteSessionAndLogoutRevokesIt() throws Exception {
        Csrf csrfToken = getCsrfToken();
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .cookie(csrfToken.cookie())
                        .header(csrfToken.headerName(), csrfToken.value())
                        .contentType("application/json")
                        .content("{\"username\":\"demo-seminar\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo-seminar"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andReturn();

        Cookie session = login.getResponse().getCookie("SESSION");
        assertNotNull(session);
        assertEquals(true, session.isHttpOnly());
        String sessionId = new String(Base64.getUrlDecoder().decode(session.getValue()), StandardCharsets.UTF_8);
        assertEquals(300, jdbc.queryForObject(
                "SELECT max_inactive_interval FROM spring_session WHERE session_id = ?",
                Integer.class,
                sessionId
        ));

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo-seminar"));
        mvc.perform(get("/api/courses").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disciplineName").value("Демонстрационная дисциплина"));

        mvc.perform(post("/api/auth/logout")
                        .cookie(session, csrfToken.cookie())
                        .header(csrfToken.headerName(), csrfToken.value()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        Csrf csrfToken = getCsrfToken();

        mvc.perform(post("/api/auth/login")
                        .cookie(csrfToken.cookie())
                        .header(csrfToken.headerName(), csrfToken.value())
                        .contentType("application/json")
                        .content("{\"username\":\"demo-seminar\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Неверный логин или пароль"));
    }

    @Test
    void loginRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"demo-seminar\",\"password\":\"demo1234\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsSessionWhoseInactivityTimeoutHasElapsed() throws Exception {
        Csrf csrfToken = getCsrfToken();
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .cookie(csrfToken.cookie())
                        .header(csrfToken.headerName(), csrfToken.value())
                        .contentType("application/json")
                        .content("{\"username\":\"demo-seminar\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie session = login.getResponse().getCookie("SESSION");
        assertNotNull(session);
        String sessionId = new String(Base64.getUrlDecoder().decode(session.getValue()), StandardCharsets.UTF_8);
        assertEquals(1, jdbc.update(
                "UPDATE spring_session SET last_access_time = 0, expiry_time = 0 WHERE session_id = ?",
                sessionId
        ));

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    private Csrf getCsrfToken() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String headerName = com.jayway.jsonpath.JsonPath.read(body, "$.headerName");
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertEquals(true, cookie.isHttpOnly());
        return new Csrf(headerName, token, cookie);
    }

    private record Csrf(String headerName, String value, Cookie cookie) {
    }
}
