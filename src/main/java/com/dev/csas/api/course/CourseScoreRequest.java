package com.dev.csas.api.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CourseScoreRequest(
        @NotNull(message = "Балл обязателен") @Min(value = 0, message = "Балл не может быть отрицательным") Integer score
) {
}
