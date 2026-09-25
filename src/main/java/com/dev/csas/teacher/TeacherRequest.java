package com.dev.csas.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
        @NotBlank(message = "Имя обязательно")
        @Size(max = 50, message = "Имя не должно превышать 50 символов")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
        String lastName,

        @Size(max = 50, message = "Отчество не должно превышать 50 символов")
        String patronymic,

        @NotBlank(message = "Кафедра обязательна")
        @Size(max = 100, message = "Название кафедры не должно превышать 100 символов")
        String department,

        @NotBlank(message = "Должность обязательна")
        @Size(max = 100, message = "Должность не должна превышать 100 символов")
        String position,

        @Size(max = 50, message = "Учёная степень не должна превышать 50 символов")
        String academicDegree,

        @Email(message = "Некорректный формат email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        String email,

        @Size(max = 20, message = "Телефон не должен превышать 20 символов")
        String phone
) {
}
