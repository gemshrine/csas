package com.dev.csas.api.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CourseWorkRequest(
        @NotBlank(message = "Название работы обязательно") String title,
        @NotNull(message = "Максимальный балл обязателен") @Min(value = 1, message = "Максимальный балл должен быть положительным") Integer maxScore,
        @NotNull(message = "Дата работы обязательна") LocalDate date
) {
}
