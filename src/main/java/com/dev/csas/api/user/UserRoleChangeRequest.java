package com.dev.csas.api.user;

import com.dev.csas.user.RoleEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserRoleChangeRequest(
        @NotNull RoleEnum role,
        @Positive Long profileId
) {
}
