package com.dev.csas.api.user;

import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserJPAEntity;

public record UserAccountResponse(
        long id,
        String username,
        RoleEnum role,
        boolean enabled,
        Long profileId,
        String profileName
) {

    public UserAccountResponse(long id, String username, RoleEnum role, boolean enabled) {
        this(id, username, role, enabled, null, null);
    }

    public UserAccountResponse(long id, String username, RoleEnum role, boolean enabled, Long profileId) {
        this(id, username, role, enabled, profileId, null);
    }

    public static UserAccountResponse from(UserJPAEntity user) {
        return new UserAccountResponse(
                user.getId(), user.getUsername(), user.getRole(), user.isEnabled(), null
        );
    }

    public static UserAccountResponse from(UserJPAEntity user, Long profileId) {
        return from(user, profileId, null);
    }

    public static UserAccountResponse from(UserJPAEntity user, Long profileId, String profileName) {
        return new UserAccountResponse(
                user.getId(), user.getUsername(), user.getRole(), user.isEnabled(), profileId, profileName
        );
    }
}
