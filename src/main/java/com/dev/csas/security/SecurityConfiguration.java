package com.dev.csas.security;

import com.dev.csas.api.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(
                                "/api/groups/**",
                                "/api/students/**",
                                "/api/teachers/**",
                                "/api/disciplines/**",
                                "/api/semesters/**"
                        ).hasRole("DEAN")
                        .requestMatchers("/api/me/courses", "/api/me/courses/*").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/courses").hasAnyRole("TEACHER", "DEAN")
                        .requestMatchers(HttpMethod.GET, "/api/courses/*").hasAnyRole("TEACHER", "DEAN")
                        .requestMatchers("/api/courses/*/works", "/api/courses/*/works/*",
                                "/api/courses/*/works/*/scores/*", "/api/courses/*/complete").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/control-sheets/*").hasAnyRole("TEACHER", "DEAN")
                        .requestMatchers(HttpMethod.GET, "/api/attendance-sheets/*").hasAnyRole("TEACHER", "DEAN")
                        .requestMatchers("/api/attendance-sheets/*/lectures").hasRole("TEACHER")
                        .requestMatchers("/api/attendance-sheets/*/lectures/*/attendance/*").hasRole("TEACHER")
                        .requestMatchers("/api/attendance-sheets/*/close").hasRole("TEACHER")
                        .requestMatchers("/api/attendance-sheets/*/archive").hasRole("DEAN")
                        .requestMatchers("/api/control-sheets/*/results/*/numeric").hasRole("TEACHER")
                        .requestMatchers("/api/control-sheets/*/results/*/pass-fail").hasRole("TEACHER")
                        .requestMatchers("/api/control-sheets/*/close").hasRole("TEACHER")
                        .requestMatchers("/api/control-sheets/*/reopen").hasRole("DEAN")
                        .requestMatchers("/api/control-sheets/*/archive").hasRole("DEAN")
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeSecurityError(
                                objectMapper,
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Требуется аутентификация",
                                request.getRequestURI()
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeSecurityError(
                                objectMapper,
                                response,
                                HttpStatus.FORBIDDEN,
                                "Недостаточно прав для выполнения операции",
                                request.getRequestURI()
                        ))
                );
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/control-sheets/**", "/attendance-sheets/**")
                .hasAnyRole("TEACHER", "DEAN")
                .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).sameSite("Lax"));
        return repository;
    }

    @Bean
    CookieSerializer sessionCookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }

    private void writeSecurityError(
            ObjectMapper objectMapper,
            HttpServletResponse response,
            HttpStatus status,
            String message,
            String path
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        ));
    }
}
