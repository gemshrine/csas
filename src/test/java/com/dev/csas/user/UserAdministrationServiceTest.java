package com.dev.csas.user;

import com.dev.csas.api.user.UserAccountResponse;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.teacher.TeacherJPAEntity;
import com.dev.csas.teacher.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private TeacherRepository teachers;
    @Mock
    private StudentRepository students;
    @Mock
    private GroupRepository groups;
    @Mock
    private FindByIndexNameSessionRepository<? extends Session> sessions;
    @InjectMocks
    private UserAdministrationService service;

    @Test
    void listsAccountsWithoutReturningPasswordData() {
        when(users.findAllByOrderByIdAsc()).thenReturn(List.of(user(3L, "teacher", RoleEnum.TEACHER)));

        var result = service.findAll();

        assertThat(result).containsExactly(new UserAccountResponse(
                3L, "teacher", RoleEnum.TEACHER, true
        ));
    }

    @Test
    void changesRoleWhenTeacherProfileIsLinkedAndRevokesSessions() {
        UserJPAEntity user = user(3L, "teacher", RoleEnum.STUDENT);
        when(users.findById(3L)).thenReturn(Optional.of(user));
        when(teachers.findByUser_Id(3L)).thenReturn(Optional.of(TeacherJPAEntity.builder().id(9L).build()));
        when(users.save(user)).thenReturn(user);
        doReturn(Map.of("session-1", mock(Session.class)))
                .when(sessions).findByPrincipalName("teacher");

        var result = service.changeRole(3L, RoleEnum.TEACHER);

        assertThat(result.role()).isEqualTo(RoleEnum.TEACHER);
        verify(sessions).deleteById("session-1");
    }

    @Test
    void refusesTeacherRoleWhenProfileIsNotLinked() {
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, "account", RoleEnum.DEAN)));
        when(teachers.findByUser_Id(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(3L, RoleEnum.TEACHER))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("не привязан профиль преподавателя");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesStudentRoleWhenProfileIsNotLinked() {
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, "account", RoleEnum.DEAN)));
        when(students.findByUser_Id(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(3L, RoleEnum.STUDENT))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("не привязан профиль студента");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsMissingAccount() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(99L, RoleEnum.DEAN))
                .isInstanceOf(CatalogNotFoundException.class)
                .hasMessageContaining("id 99");
    }

    @Test
    void doesNotRevokeSessionsWhenRoleIsUnchanged() {
        UserJPAEntity user = user(3L, "dean", RoleEnum.DEAN);
        when(users.findById(3L)).thenReturn(Optional.of(user));

        var result = service.changeRole(3L, RoleEnum.DEAN);

        assertThat(result.role()).isEqualTo(RoleEnum.DEAN);
        verify(sessions, never()).findByPrincipalName("dean");
    }

    @Test
    void refusesToDemoteTheLastEnabledAdministrator() {
        UserJPAEntity user = user(3L, "admin", RoleEnum.ADMIN);
        when(users.findById(3L)).thenReturn(Optional.of(user));
        when(users.countByRoleAndEnabledTrue(RoleEnum.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(3L, RoleEnum.DEAN))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessage("Нельзя изменить роль последнего активного администратора");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refusesToChangeRoleOfAssignedGroupCurator() {
        when(users.findById(3L)).thenReturn(Optional.of(user(3L, "dean", RoleEnum.DEAN)));
        when(groups.existsByCurator_Id(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.changeRole(3L, RoleEnum.ADMIN))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("назначен куратором");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private UserJPAEntity user(long id, String username, RoleEnum role) {
        return UserJPAEntity.builder()
                .id(id)
                .username(username)
                .password("password-hash")
                .role(role)
                .enabled(true)
                .build();
    }
}
