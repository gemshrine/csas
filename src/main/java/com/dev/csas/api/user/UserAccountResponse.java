package com.dev.csas.api.user;

import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserJPAEntity;

public record UserAccountResponse(long id, String username, RoleEnum role, boolean enabled) {

    public static UserAccountResponse from(UserJPAEntity user) {
        return new UserAccountResponse(
                user.getId(), user.getUsername(), user.getRole(), user.isEnabled()
        );
    }
}
