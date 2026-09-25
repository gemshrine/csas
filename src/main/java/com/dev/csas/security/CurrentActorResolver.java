package com.dev.csas.security;

import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.TeacherID;
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

    public CurrentActorResolver(UserRepository users, TeacherRepository teachers) {
        this.users = users;
        this.teachers = teachers;
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

    private UserJPAEntity findCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ActorAccessDeniedException("Пользователь не аутентифицирован");
        }
        return users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ActorAccessDeniedException("Учётная запись пользователя не найдена"));
    }
}
