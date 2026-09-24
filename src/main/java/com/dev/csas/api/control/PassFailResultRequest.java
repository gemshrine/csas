package com.dev.csas.api.control;

import com.dev.csas.control.PassFail;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PassFailResultRequest(
        @NotNull PassFail result,
        @Size(max = 500) String reason
) {
}
