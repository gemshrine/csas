package com.dev.csas.api.user;

import com.dev.csas.user.RoleEnum;
import jakarta.validation.constraints.NotNull;

public record UserRoleChangeRequest(@NotNull RoleEnum role) {
}
