package com.dev.csas.user;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.api.user.UserAccountResponse;
import com.dev.csas.api.user.UserAccountPatchRequest;
import com.dev.csas.api.user.UserCreationRequest;
import com.dev.csas.api.user.UserProfileOptionsResponse;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.teacher.TeacherJPAEntity;
import com.dev.csas.teacher.TeacherRepository;
import com.dev.csas.student.StudentJPAEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    @Mock
    private UserActionService userActions;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserAdministrationService service;

    @Test
    void listsOnlyUnlinkedTeacherAndStudentProfiles() {
        when(teachers.findAllByUserIsNullOrderByIdAsc()).thenReturn(List.of(
                TeacherJPAEntity.builder().id(7L).firstName("Пётр").lastName("Петров")
                        .patronymic("Сергеевич").build()
        ));
        when(students.findAllByUserIsNullOrderByIdAsc()).thenReturn(List.of(
                StudentJPAEntity.builder().id(15L).recordBookNumber("TST0000015")
                        .firstName("Иван").lastName("Иванов").patronymic("Иванович").build()
        ));

        UserProfileOptionsResponse result = service.findAvailableProfiles();

        assertThat(result.teachers()).containsExactly(
                new UserProfileOptionsResponse.ProfileOption(7L, "Петров Пётр Сергеевич")
        );
        assertThat(result.students()).containsExactly(
                new UserProfileOptionsResponse.ProfileOption(15L, "TST0000015 Иванов Иван Иванович")
        );
    }

    @Test
    void listsAccountsWithoutReturningPasswordData() {
        when(users.findAllByOrderByIdAsc()).thenReturn(List.of(user(3L, "teacher", RoleEnum.TEACHER)));

        var result = service.findAll();

        assertThat(result).containsExactly(new UserAccountResponse(
                3L, "teacher", RoleEnum.TEACHER, true
        ));
    }

    @Test
    void includesLinkedProfileIdInAccountList() {
        when(users.findAllByOrderByIdAsc()).thenReturn(List.of(user(3L, "teacher", RoleEnum.TEACHER)));
        when(teachers.findByUser_Id(3L)).thenReturn(Optional.of(TeacherJPAEntity.builder()
                .id(9L).lastName("Петров").firstName("Пётр").patronymic("Сергеевич").build()));

        var result = service.findAll();

        assertThat(result.getFirst().profileId()).isEqualTo(9L);
        assertThat(result.getFirst().profileName()).isEqualTo("Петров П.С.");
    }

    @Test
    void createsDeanAccountWithEncodedPassword() {
        when(users.findByUsername("dean")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(users.saveAndFlush(org.mockito.ArgumentMatchers.any(UserJPAEntity.class))).thenAnswer(invocation -> {
            UserJPAEntity saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var result = service.create(new UserCreationRequest("dean", "password123", RoleEnum.DEAN, null));

        assertThat(result).isEqualTo(new UserAccountResponse(10L, "dean", RoleEnum.DEAN, true));
        verify(passwordEncoder).encode("password123");
        verify(userActions).record(UserActionType.ACCOUNT_CREATED, "USER", 10L, "DEAN");
    }

    @Test
    void createsStudentAccountAndLinksExistingProfile() {
        StudentJPAEntity student = StudentJPAEntity.builder().id(4L).build();
        when(users.findByUsername("student")).thenReturn(Optional.empty());
        when(students.findById(4L)).thenReturn(Optional.of(student));
        when(students.findByUser_Id(11L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(users.saveAndFlush(org.mockito.ArgumentMatchers.any(UserJPAEntity.class))).thenAnswer(invocation -> {
            UserJPAEntity saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        var result = service.create(new UserCreationRequest("student", "password123", RoleEnum.STUDENT, 4L));

        assertThat(student.getUser()).isNotNull();
        assertThat(student.getUser().getId()).isEqualTo(11L);
        assertThat(student.getUser().getRole()).isEqualTo(RoleEnum.STUDENT);
        assertThat(result.profileId()).isEqualTo(4L);
        verify(students).saveAndFlush(student);
        verify(userActions).record(UserActionType.ACCOUNT_CREATED, "USER", 11L, "STUDENT, профиль 4");
    }

    @Test
    void linksProfileWhenChangingAccountRoleToStudent() {
        UserJPAEntity user = user(15L, "account", RoleEnum.DEAN);
        StudentJPAEntity student = StudentJPAEntity.builder().id(4L).build();
        when(users.findById(15L)).thenReturn(Optional.of(user));
        when(groups.existsByCurator_Id(15L)).thenReturn(false);
        when(students.findById(4L)).thenReturn(Optional.of(student));
        when(teachers.findByUser_Id(15L)).thenReturn(Optional.empty());
        when(users.save(user)).thenReturn(user);
        doReturn(Map.of()).when(sessions).findByPrincipalName("account");

        var result = service.changeRole(15L, RoleEnum.STUDENT, 4L);

        assertThat(result.role()).isEqualTo(RoleEnum.STUDENT);
        assertThat(student.getUser()).isSameAs(user);
        verify(students).saveAndFlush(student);
    }

    @Test
    void refusesCreatingAccountForAlreadyLinkedTeacher() {
        TeacherJPAEntity teacher = TeacherJPAEntity.builder()
                .id(7L)
                .user(user(12L, "existing", RoleEnum.TEACHER))
                .build();
        when(users.findByUsername("teacher")).thenReturn(Optional.empty());
        when(teachers.findById(7L)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.create(
                new UserCreationRequest("teacher", "password123", RoleEnum.TEACHER, 7L)
        )).isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("уже привязана");
        verify(users, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
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
        verify(userActions).record(UserActionType.ACCOUNT_ROLE_CHANGED, "USER", 3L,
                "Роль изменена с STUDENT на TEACHER; профиль изменён с нет на преподаватель #9");
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

    @Test
    void disablesAccountAndRevokesItsSessions() {
        UserJPAEntity user = user(5L, "teacher", RoleEnum.TEACHER);
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        doReturn(Map.of("session-1", mock(Session.class)))
                .when(sessions).findByPrincipalName("teacher");

        var result = service.update(5L, new UserAccountPatchRequest(null, null, false), "admin");

        assertThat(result.enabled()).isFalse();
        verify(sessions).deleteById("session-1");
        verify(userActions).record(UserActionType.ACCOUNT_UPDATED, "USER", 5L,
                "Изменены поля: доступ");
    }

    @Test
    void logsPasswordChangeWithoutStoringThePassword() {
        UserJPAEntity user = user(5L, "teacher", RoleEnum.TEACHER);
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");
        when(users.save(user)).thenReturn(user);
        doReturn(Map.of()).when(sessions).findByPrincipalName("teacher");

        service.update(5L, new UserAccountPatchRequest(null, "new-password", null), "admin");

        verify(userActions).record(UserActionType.ACCOUNT_UPDATED, "USER", 5L,
                "Изменены поля: пароль");
    }

    @Test
    void refusesDisablingLastEnabledAdministrator() {
        UserJPAEntity user = user(5L, "admin", RoleEnum.ADMIN);
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(users.countByRoleAndEnabledTrue(RoleEnum.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(5L,
                new UserAccountPatchRequest(null, null, false), "another-admin"))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("последнего активного администратора");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletesAccountAndDetachesTeacherProfile() {
        UserJPAEntity user = user(8L, "teacher", RoleEnum.TEACHER);
        TeacherJPAEntity teacher = TeacherJPAEntity.builder().id(2L).user(user).build();
        when(users.findById(8L)).thenReturn(Optional.of(user));
        when(groups.existsByCurator_Id(8L)).thenReturn(false);
        when(teachers.findByUser_Id(8L)).thenReturn(Optional.of(teacher));
        when(students.findByUser_Id(8L)).thenReturn(Optional.empty());
        doReturn(Map.of()).when(sessions).findByPrincipalName("teacher");

        service.delete(8L, "admin");

        assertThat(teacher.getUser()).isNull();
        verify(teachers).saveAndFlush(teacher);
        verify(users).delete(user);
        verify(userActions).record(UserActionType.ACCOUNT_DELETED, "USER", 8L,
                "Удалена учётная запись с ролью TEACHER");
    }

    @Test
    void changesLinkedProfileWithoutChangingAccountRole() {
        UserJPAEntity user = user(15L, "student", RoleEnum.STUDENT);
        StudentJPAEntity oldProfile = StudentJPAEntity.builder().id(4L).user(user).build();
        StudentJPAEntity newProfile = StudentJPAEntity.builder().id(5L).build();
        when(users.findById(15L)).thenReturn(Optional.of(user));
        when(students.findByUser_Id(15L)).thenReturn(
                Optional.of(oldProfile), Optional.of(oldProfile), Optional.of(newProfile)
        );
        when(students.findById(5L)).thenReturn(Optional.of(newProfile));
        when(users.save(user)).thenReturn(user);
        doReturn(Map.of()).when(sessions).findByPrincipalName("student");

        var result = service.changeRole(15L, RoleEnum.STUDENT, 5L);

        assertThat(result.profileId()).isEqualTo(5L);
        assertThat(oldProfile.getUser()).isNull();
        assertThat(newProfile.getUser()).isSameAs(user);
        verify(userActions).record(UserActionType.ACCOUNT_PROFILE_CHANGED, "USER", 15L,
                "Профиль изменён с студент #4 на студент #5");
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
