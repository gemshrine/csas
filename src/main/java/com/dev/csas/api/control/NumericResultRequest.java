package com.dev.csas.api.control;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NumericResultRequest(
        @NotNull @Min(0) @Max(100) Integer score,
        @Size(max = 500) String reason
) {
}
