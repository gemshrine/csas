package com.dev.csas.security;

import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserJPAEntity;
import com.dev.csas.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseUserDetailsServiceTest {

    @Test
    void loadsEnabledAccountAndRoleUsingStoredPasswordHash() {
        UserRepository users = mock(UserRepository.class);
        String passwordHash = new BCryptPasswordEncoder().encode("password-123");
        UserJPAEntity user = UserJPAEntity.builder()
                .id(3L)
                .username("teacher")
                .password(passwordHash)
                .role(RoleEnum.TEACHER)
                .enabled(true)
                .build();
        when(users.findByUsername("teacher")).thenReturn(Optional.of(user));

        UserDetails details = new DatabaseUserDetailsService(users).loadUserByUsername("teacher");

        assertThat(details.getUsername()).isEqualTo("teacher");
        assertThat(details.getPassword()).isEqualTo(passwordHash);
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_TEACHER");
        assertThat(new BCryptPasswordEncoder().matches("password-123", details.getPassword())).isTrue();
    }
}
