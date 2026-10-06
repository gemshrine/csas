package com.dev.csas.user;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.api.user.UserAccountPatchRequest;
import com.dev.csas.api.user.UserAccountResponse;
import com.dev.csas.api.user.UserCreationRequest;
import com.dev.csas.api.user.UserProfileOptionsResponse;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.student.StudentJPAEntity;
import com.dev.csas.teacher.TeacherRepository;
import com.dev.csas.teacher.TeacherJPAEntity;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Service
public class UserAdministrationService {

    private final UserRepository users;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final GroupRepository groups;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;
    private final UserActionService userActions;
    private final PasswordEncoder passwordEncoder;

    public UserAdministrationService(
            UserRepository users,
            TeacherRepository teachers,
            StudentRepository students,
            GroupRepository groups,
            FindByIndexNameSessionRepository<? extends Session> sessions,
            UserActionService userActions,
            PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.teachers = teachers;
        this.students = students;
        this.groups = groups;
        this.sessions = sessions;
        this.userActions = userActions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAccountResponse> findAll() {
        return users.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileOptionsResponse findAvailableProfiles() {
        List<UserProfileOptionsResponse.ProfileOption> teacherProfiles = teachers
                .findAllByUserIsNullOrderByIdAsc().stream()
                .map(teacher -> new UserProfileOptionsResponse.ProfileOption(
                        teacher.getId(), profileName(teacher.getLastName(), teacher.getFirstName(), teacher.getPatronymic())
                ))
                .toList();
        List<UserProfileOptionsResponse.ProfileOption> studentProfiles = students
                .findAllByUserIsNullOrderByIdAsc().stream()
                .map(student -> new UserProfileOptionsResponse.ProfileOption(
                        student.getId(), student.getRecordBookNumber() + " "
                        + profileName(student.getLastName(), student.getFirstName(), student.getPatronymic())
                ))
                .toList();
        return new UserProfileOptionsResponse(teacherProfiles, studentProfiles);
    }

    @Transactional
    public UserAccountResponse create(UserCreationRequest request) {
        String username = request.username().trim();
        if (users.findByUsername(username).isPresent()) {
            throw new CatalogConflictException("Пользователь с таким логином уже существует");
        }

        TeacherJPAEntity teacher = null;
        StudentJPAEntity student = null;
        if (request.role() == RoleEnum.TEACHER) {
            if (request.profileId() == null) {
                throw new CatalogBadRequestException("Для роли TEACHER необходимо указать profileId преподавателя");
            }
            teacher = teachers.findById(request.profileId())
                    .orElseThrow(() -> CatalogNotFoundException.teacher(request.profileId()));
            if (teacher.getUser() != null) {
                throw new CatalogConflictException("К профилю преподавателя уже привязана учётная запись");
            }
        } else if (request.role() == RoleEnum.STUDENT) {
            if (request.profileId() == null) {
                throw new CatalogBadRequestException("Для роли STUDENT необходимо указать profileId студента");
            }
            student = students.findById(request.profileId())
                    .orElseThrow(() -> CatalogNotFoundException.student(request.profileId()));
            if (student.getUser() != null) {
                throw new CatalogConflictException("К профилю студента уже привязана учётная запись");
            }
        } else if (request.profileId() != null) {
            throw new CatalogBadRequestException("Для роли ADMIN или DEAN profileId указывать не нужно");
        }

        UserJPAEntity user = users.saveAndFlush(UserJPAEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build());

        if (teacher != null) {
            teacher.setUser(user);
            teachers.saveAndFlush(teacher);
        }
        if (student != null) {
            student.setUser(user);
            students.saveAndFlush(student);
        }
        String details = request.role().name();
        if (request.profileId() != null) {
            details += ", профиль " + request.profileId();
        }
        userActions.record(UserActionType.ACCOUNT_CREATED, "USER", user.getId(), details);
        return toResponse(user);
    }

    @Transactional
    public UserAccountResponse update(long userId, UserAccountPatchRequest request, String actorUsername) {
        UserJPAEntity user = findUser(userId);
        String previousUsername = user.getUsername();
        boolean changed = false;
        var changedFields = new ArrayList<String>();

        if (request.username() != null) {
            String username = request.username().trim();
            if (username.isBlank()) {
                throw new CatalogBadRequestException("Логин не может быть пустым");
            }
            if (!username.equals(user.getUsername()) && users.existsByUsernameAndIdNot(username, userId)) {
                throw new CatalogConflictException("Пользователь с таким логином уже существует");
            }
            if (!username.equals(user.getUsername())) {
                user.setUsername(username);
                changed = true;
                changedFields.add("логин");
            }
        }

        if (request.password() != null) {
            if (request.password().isBlank()) {
                throw new CatalogBadRequestException("Пароль не может быть пустым");
            }
            user.setPassword(passwordEncoder.encode(request.password()));
            changed = true;
            changedFields.add("пароль");
        }

        if (request.enabled() != null && request.enabled() != user.isEnabled()) {
            if (!request.enabled()) {
                ensureNotLastEnabledAdministrator(user);
                if (actorUsername.equals(previousUsername)) {
                    throw new CatalogConflictException("Нельзя отключить собственную учётную запись");
                }
            }
            user.setEnabled(request.enabled());
            changed = true;
            changedFields.add("доступ");
        }

        if (!changed) {
            if (request.username() == null && request.password() == null && request.enabled() == null) {
                throw new CatalogBadRequestException("Не указаны поля для изменения");
            }
            return toResponse(user);
        }

        UserJPAEntity saved = users.save(user);
        revokeSessions(previousUsername);
        if (!previousUsername.equals(user.getUsername())) {
            revokeSessions(user.getUsername());
        }
        userActions.record(UserActionType.ACCOUNT_UPDATED, "USER", userId,
                "Изменены поля: " + String.join(", ", changedFields));
        return toResponse(saved);
    }

    @Transactional
    public UserAccountResponse changeRole(long userId, RoleEnum role) {
        return changeRole(userId, role, null);
    }

    @Transactional
    public UserAccountResponse changeRole(long userId, RoleEnum role, Long profileId) {
        UserJPAEntity user = findUser(userId);

        if (user.getRole() == role && profileId == null) {
            return toResponse(user);
        }
        if (user.getRole() == RoleEnum.DEAN && role != RoleEnum.DEAN && groups.existsByCurator_Id(userId)) {
            throw new CatalogConflictException(
                    "Нельзя изменить роль: пользователь назначен куратором одной или нескольких групп"
            );
        }
        if (user.getRole() == RoleEnum.ADMIN && role != RoleEnum.ADMIN
                && user.isEnabled() && users.countByRoleAndEnabledTrue(RoleEnum.ADMIN) == 1) {
            throw new CatalogConflictException(
                    "Нельзя изменить роль последнего активного администратора"
            );
        }
        RoleEnum oldRole = user.getRole();
        Long oldProfileId = profileIdFor(userId, oldRole);
        if ((role == RoleEnum.TEACHER || role == RoleEnum.STUDENT) && profileId != null) {
            linkProfile(user, role, profileId);
        } else if (role == RoleEnum.TEACHER || role == RoleEnum.STUDENT) {
            validateProfileLink(userId, role);
        } else if (profileId != null) {
            throw new CatalogBadRequestException("Для роли ADMIN или DEAN profileId указывать не нужно");
        }
        if (role != RoleEnum.TEACHER) {
            teachers.findByUser_Id(userId).ifPresent(teacher -> {
                teacher.setUser(null);
                teachers.saveAndFlush(teacher);
            });
        }
        if (role != RoleEnum.STUDENT) {
            students.findByUser_Id(userId).ifPresent(student -> {
                student.setUser(null);
                students.saveAndFlush(student);
            });
        }
        user.setRole(role);
        UserJPAEntity saved = users.save(user);
        revokeSessions(user.getUsername());
        Long newProfileId = profileIdFor(userId, role);
        boolean roleChanged = oldRole != role;
        boolean profileChanged = !Objects.equals(oldProfileId, newProfileId);
        if (roleChanged) {
            String details = "Роль изменена с " + oldRole.name() + " на " + role.name();
            if (profileChanged) {
                details += "; профиль изменён с " + profileLabel(oldRole, oldProfileId)
                        + " на " + profileLabel(role, newProfileId);
            }
            userActions.record(UserActionType.ACCOUNT_ROLE_CHANGED, "USER", userId,
                    details);
        } else if (profileChanged) {
            userActions.record(UserActionType.ACCOUNT_PROFILE_CHANGED, "USER", userId,
                    "Профиль изменён с " + profileLabel(oldRole, oldProfileId)
                            + " на " + profileLabel(role, newProfileId));
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(long userId, String actorUsername) {
        UserJPAEntity user = findUser(userId);
        if (actorUsername.equals(user.getUsername())) {
            throw new CatalogConflictException("Нельзя удалить собственную учётную запись");
        }
        ensureNotLastEnabledAdministrator(user);
        if (groups.existsByCurator_Id(userId)) {
            throw new CatalogConflictException("Сначала снимите пользователя с должности куратора группы");
        }

        teachers.findByUser_Id(userId).ifPresent(teacher -> {
            teacher.setUser(null);
            teachers.saveAndFlush(teacher);
        });
        students.findByUser_Id(userId).ifPresent(student -> {
            student.setUser(null);
            students.saveAndFlush(student);
        });
        users.delete(user);
        users.flush();
        revokeSessions(user.getUsername());
        userActions.record(UserActionType.ACCOUNT_DELETED, "USER", userId,
                "Удалена учётная запись с ролью " + user.getRole().name());
    }

    private void validateProfileLink(long userId, RoleEnum role) {
        if (role == RoleEnum.TEACHER && teachers.findByUser_Id(userId).isEmpty()) {
            throw new CatalogConflictException(
                    "Нельзя назначить роль TEACHER: к учётной записи не привязан профиль преподавателя"
            );
        }
        if (role == RoleEnum.STUDENT && students.findByUser_Id(userId).isEmpty()) {
            throw new CatalogConflictException(
                    "Нельзя назначить роль STUDENT: к учётной записи не привязан профиль студента"
            );
        }
    }

    private void linkProfile(UserJPAEntity user, RoleEnum role, long profileId) {
        long userId = user.getId();
        if (role == RoleEnum.TEACHER) {
            TeacherJPAEntity profile = teachers.findById(profileId)
                    .orElseThrow(() -> CatalogNotFoundException.teacher(profileId));
            if (profile.getUser() != null && !profile.getUser().getId().equals(userId)) {
                throw new CatalogConflictException("К профилю преподавателя уже привязана другая учётная запись");
            }
            teachers.findByUser_Id(userId).ifPresent(currentProfile -> {
                if (!currentProfile.getId().equals(profileId)) {
                    currentProfile.setUser(null);
                    teachers.saveAndFlush(currentProfile);
                }
            });
            students.findByUser_Id(userId).ifPresent(student -> {
                student.setUser(null);
                students.saveAndFlush(student);
            });
            profile.setUser(user);
            teachers.saveAndFlush(profile);
        } else {
            StudentJPAEntity profile = students.findById(profileId)
                    .orElseThrow(() -> CatalogNotFoundException.student(profileId));
            if (profile.getUser() != null && !profile.getUser().getId().equals(userId)) {
                throw new CatalogConflictException("К профилю студента уже привязана другая учётная запись");
            }
            students.findByUser_Id(userId).ifPresent(currentProfile -> {
                if (!currentProfile.getId().equals(profileId)) {
                    currentProfile.setUser(null);
                    students.saveAndFlush(currentProfile);
                }
            });
            teachers.findByUser_Id(userId).ifPresent(teacher -> {
                teacher.setUser(null);
                teachers.saveAndFlush(teacher);
            });
            profile.setUser(user);
            students.saveAndFlush(profile);
        }
    }

    private UserJPAEntity findUser(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new CatalogNotFoundException(
                        "Учётная запись с id " + userId + " не найдена"
                ));
    }

    private void ensureNotLastEnabledAdministrator(UserJPAEntity user) {
        if (user.getRole() == RoleEnum.ADMIN && user.isEnabled()
                && users.countByRoleAndEnabledTrue(RoleEnum.ADMIN) == 1) {
            throw new CatalogConflictException("Нельзя отключить или удалить последнего активного администратора");
        }
    }

    private void revokeSessions(String username) {
        sessions.findByPrincipalName(username).keySet().forEach(sessions::deleteById);
    }

    private UserAccountResponse toResponse(UserJPAEntity user) {
        return switch (user.getRole()) {
            case TEACHER -> teachers.findByUser_Id(user.getId())
                    .map(teacher -> UserAccountResponse.from(user, teacher.getId(), profileInitials(
                            teacher.getLastName(), teacher.getFirstName(), teacher.getPatronymic()
                    )))
                    .orElseGet(() -> UserAccountResponse.from(user, null, null));
            case STUDENT -> students.findByUser_Id(user.getId())
                    .map(student -> UserAccountResponse.from(user, student.getId(), profileInitials(
                            student.getLastName(), student.getFirstName(), student.getPatronymic()
                    )))
                    .orElseGet(() -> UserAccountResponse.from(user, null, null));
            case ADMIN, DEAN -> UserAccountResponse.from(user, null, null);
        };
    }

    private Long profileIdFor(long userId, RoleEnum role) {
        return switch (role) {
            case TEACHER -> teachers.findByUser_Id(userId).map(TeacherJPAEntity::getId).orElse(null);
            case STUDENT -> students.findByUser_Id(userId).map(StudentJPAEntity::getId).orElse(null);
            case ADMIN, DEAN -> null;
        };
    }

    private String profileLabel(RoleEnum role, Long profileId) {
        if (profileId == null) {
            return "нет";
        }
        return switch (role) {
            case TEACHER -> "преподаватель #" + profileId;
            case STUDENT -> "студент #" + profileId;
            case ADMIN, DEAN -> "нет";
        };
    }

    private String profileName(String lastName, String firstName, String patronymic) {
        return List.of(lastName, firstName, patronymic == null ? "" : patronymic).stream()
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String profileInitials(String lastName, String firstName, String patronymic) {
        String surname = lastName == null ? "" : lastName.trim();
        String initials = initial(firstName) + initial(patronymic);
        String result = surname.isEmpty() ? initials : initials.isEmpty() ? surname : surname + " " + initials;
        return result.isEmpty() ? null : result;
    }

    private String initial(String value) {
        return value == null || value.isBlank()
                ? ""
                : Character.toUpperCase(value.trim().charAt(0)) + ".";
    }
}
