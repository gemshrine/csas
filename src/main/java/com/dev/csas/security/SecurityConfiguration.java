package com.dev.csas.security;

import com.dev.csas.api.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
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
                .httpBasic(Customizer.withDefaults())
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
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"csas-api\"");
        }
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        ));
    }
}
