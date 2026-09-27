package com.dev.csas.security;

import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.teacher.TeacherRepository;
import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserJPAEntity;
import com.dev.csas.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentActorResolver {

    private final UserRepository users;
    private final TeacherRepository teachers;
    private final StudentRepository students;

    public CurrentActorResolver(UserRepository users, TeacherRepository teachers, StudentRepository students) {
        this.users = users;
        this.teachers = teachers;
        this.students = students;
    }

    public TeacherID teacher(Authentication authentication) {
        UserJPAEntity user = findCurrentUser(authentication);
        if (user.getRole() != RoleEnum.TEACHER) {
            throw new ActorAccessDeniedException("Операция доступна только преподавателю");
        }
        return teachers.findByUser_Id(user.getId())
                .map(teacher -> new TeacherID(teacher.getId()))
                .orElseThrow(() -> new ActorAccessDeniedException(
                        "Для пользователя не найдена учётная запись преподавателя"
                ));
    }

    public DeanID dean(Authentication authentication) {
        UserJPAEntity user = findCurrentUser(authentication);
        if (user.getRole() != RoleEnum.DEAN) {
            throw new ActorAccessDeniedException("Операция доступна только деканату");
        }
        return new DeanID(user.getId());
    }

    public StudentID student(Authentication authentication) {
        UserJPAEntity user = findCurrentUser(authentication);
        if (user.getRole() != RoleEnum.STUDENT) {
            throw new ActorAccessDeniedException("Операция доступна только студенту");
        }
        return students.findByUser_Id(user.getId())
                .map(student -> new StudentID(student.getId()))
                .orElseThrow(() -> new ActorAccessDeniedException(
                        "Для пользователя не найдена учётная запись студента"
                ));
    }

    private UserJPAEntity findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ActorAccessDeniedException("Пользователь не аутентифицирован");
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ActorAccessDeniedException("Учётная запись пользователя не найдена"));
    }
}
