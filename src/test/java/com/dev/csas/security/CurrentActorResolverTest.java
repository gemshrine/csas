package com.dev.csas.security;

import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentJPAEntity;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.teacher.TeacherJPAEntity;
import com.dev.csas.teacher.TeacherRepository;
import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserJPAEntity;
import com.dev.csas.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentActorResolverTest {

    private final UserRepository users = mock(UserRepository.class);
    private final TeacherRepository teachers = mock(TeacherRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final CurrentActorResolver resolver = new CurrentActorResolver(users, teachers, students);
    private final Authentication authentication = mock(Authentication.class);

    @BeforeEach
    void setUp() {
        when(authentication.isAuthenticated()).thenReturn(true);
    }

    @Test
    void resolvesTeacherProfileIdFromAuthenticatedUsername() {
        UserJPAEntity user = user(14, RoleEnum.TEACHER);
        TeacherJPAEntity teacher = TeacherJPAEntity.builder().id(72L).user(user).build();
        when(authentication.getName()).thenReturn("teacher");
        when(users.findByUsername("teacher")).thenReturn(Optional.of(user));
        when(teachers.findByUser_Id(14L)).thenReturn(Optional.of(teacher));

        assertThat(resolver.teacher(authentication)).isEqualTo(new TeacherID(72));
    }

    @Test
    void resolvesDeanIdFromAuthenticatedUserId() {
        UserJPAEntity user = user(21, RoleEnum.DEAN);
        when(authentication.getName()).thenReturn("dean");
        when(users.findByUsername("dean")).thenReturn(Optional.of(user));

        assertThat(resolver.dean(authentication)).isEqualTo(new DeanID(21));
    }

    @Test
    void resolvesStudentProfileIdFromAuthenticatedUsername() {
        UserJPAEntity user = user(32, RoleEnum.STUDENT);
        StudentJPAEntity student = StudentJPAEntity.builder().id(81L).user(user).build();
        when(authentication.getName()).thenReturn("student");
        when(users.findByUsername("student")).thenReturn(Optional.of(user));
        when(students.findByUser_Id(32L)).thenReturn(Optional.of(student));

        assertThat(resolver.student(authentication)).isEqualTo(new StudentID(81));
    }

    @Test
    void refusesDeanWhenTeacherActorIsRequired() {
        UserJPAEntity user = user(21, RoleEnum.DEAN);
        when(authentication.getName()).thenReturn("dean");
        when(users.findByUsername("dean")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> resolver.teacher(authentication))
                .isInstanceOf(ActorAccessDeniedException.class);
    }

    private UserJPAEntity user(long id, RoleEnum role) {
        return UserJPAEntity.builder()
                .id(id)
                .username(role.name().toLowerCase())
                .password("encoded-password")
                .role(role)
                .enabled(true)
                .build();
    }
}
