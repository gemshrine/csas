package com.dev.csas.api.user;

import jakarta.validation.constraints.Size;

public record UserAccountPatchRequest(
        @Size(min = 3, max = 50)
        String username,

        @Size(min = 8, max = 100)
        String password,

        Boolean enabled
) {
}
