package com.dev.csas.group;

import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.enums.StudyFormEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroupRequest(
        @NotBlank(message = "Название группы обязательно")
        @Size(max = 50, message = "Название группы не должно превышать 50 символов")
        String name,

        @NotNull(message = "Год набора обязателен")
        @Min(value = 2000, message = "Год набора не может быть раньше 2000")
        @Max(value = 2100, message = "Год набора не может быть позже 2100")
        Integer yearOfAdmission,

        @NotBlank(message = "Специальность обязательна")
        @Size(max = 100, message = "Специальность не должна превышать 100 символов")
        String specialty,

        @NotNull(message = "Курс обязателен")
        @Min(value = 1, message = "Курс должен быть от 1 до 6")
        @Max(value = 6, message = "Курс должен быть от 1 до 6")
        Integer course,

        @NotNull(message = "Форма обучения обязательна")
        EducationFormEnum educationForm,

        @NotNull(message = "Форма занятий обязательна")
        StudyFormEnum studyForm
) {
}
