package com.dev.csas.user;

import com.dev.csas.api.user.UserAccountResponse;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.teacher.TeacherRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdministrationService {

    private final UserRepository users;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final GroupRepository groups;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public UserAdministrationService(
            UserRepository users,
            TeacherRepository teachers,
            StudentRepository students,
            GroupRepository groups,
            FindByIndexNameSessionRepository<? extends Session> sessions
    ) {
        this.users = users;
        this.teachers = teachers;
        this.students = students;
        this.groups = groups;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<UserAccountResponse> findAll() {
        return users.findAllByOrderByIdAsc().stream()
                .map(UserAccountResponse::from)
                .toList();
    }

    @Transactional
    public UserAccountResponse changeRole(long userId, RoleEnum role) {
        UserJPAEntity user = users.findById(userId)
                .orElseThrow(() -> new CatalogNotFoundException(
                        "Учётная запись с id " + userId + " не найдена"
                ));

        if (user.getRole() == role) {
            return UserAccountResponse.from(user);
        }
        if (user.getRole() == RoleEnum.DEAN && role != RoleEnum.DEAN && groups.existsByCurator_Id(userId)) {
            throw new CatalogConflictException(
                    "Нельзя изменить роль: пользователь назначен куратором одной или нескольких групп"
            );
        }
        validateProfileLink(userId, role);
        if (user.getRole() == RoleEnum.ADMIN && role != RoleEnum.ADMIN
                && user.isEnabled() && users.countByRoleAndEnabledTrue(RoleEnum.ADMIN) == 1) {
            throw new CatalogConflictException(
                    "Нельзя изменить роль последнего активного администратора"
            );
        }

        user.setRole(role);
        UserJPAEntity saved = users.save(user);
        sessions.findByPrincipalName(user.getUsername()).keySet().forEach(sessions::deleteById);
        return UserAccountResponse.from(saved);
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
}
