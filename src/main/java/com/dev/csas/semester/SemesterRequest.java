package com.dev.csas.semester;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SemesterRequest(
        @NotBlank(message = "Название семестра обязательно")
        @Size(max = 100, message = "Название семестра не должно превышать 100 символов")
        String name,

        @NotNull(message = "Дата начала обязательна")
        LocalDate startDate,

        @NotNull(message = "Дата окончания обязательна")
        LocalDate endDate
) {
}
