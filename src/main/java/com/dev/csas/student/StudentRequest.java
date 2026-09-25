package com.dev.csas.student;

import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.enums.GenderEnum;
import com.dev.csas.enums.StudentStatusEnum;
import com.dev.csas.enums.StudyFormEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @NotBlank(message = "Имя обязательно")
        @Size(max = 50, message = "Имя не должно превышать 50 символов")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
        String lastName,

        @Size(max = 50, message = "Отчество не должно превышать 50 символов")
        String patronymic,

        @NotBlank(message = "Номер зачётки обязателен")
        @Size(min = 6, max = 10, message = "Номер зачётки должен содержать от 6 до 10 символов")
        @Pattern(regexp = "^[A-Z0-9]+$", message = "Номер зачётки может содержать только заглавные буквы и цифры")
        String recordBookNumber,

        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @Email(message = "Некорректный формат email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        String email,

        @Size(max = 20, message = "Телефон не должен превышать 20 символов")
        String phone,

        GenderEnum gender,

        EducationFormEnum educationForm,

        StudyFormEnum studyForm,

        @PastOrPresent(message = "Дата поступления не может быть в будущем")
        LocalDate admissionDate,

        StudentStatusEnum status,

        Long groupId
) {
}
