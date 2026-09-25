package com.dev.csas.discipline;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisciplineRequest(
        @NotBlank(message = "Название дисциплины обязательно")
        @Size(max = 100, message = "Название не должно превышать 100 символов")
        String name,

        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description,

        @NotNull(message = "Количество часов лекций обязательно")
        @Min(value = 0, message = "Часы лекций не могут быть отрицательными")
        Integer lectureHours,

        @NotNull(message = "Количество часов практик обязательно")
        @Min(value = 0, message = "Часы практик не могут быть отрицательными")
        Integer practiceHours,

        @NotNull(message = "Количество часов лабораторных обязательно")
        @Min(value = 0, message = "Часы лабораторных не могут быть отрицательными")
        Integer labHours,

        @NotNull(message = "Форма контроля обязательна")
        ControlFormEnum controlForm
) {
}
