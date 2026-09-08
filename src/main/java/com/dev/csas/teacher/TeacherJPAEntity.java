package com.dev.csas.teacher;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TeacherJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Size(max = 50, message = "Отчество не должно превышать 50 символов")
    @Column(name = "patronymic", length = 50)
    private String patronymic;

    @NotBlank(message = "Кафедра обязательна")
    @Size(max = 100, message = "Название кафедры не должно превышать 100 символов")
    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @NotBlank(message = "Должность обязательна")
    @Size(max = 100, message = "Должность не должна превышать 100 символов")
    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @Size(max = 50, message = "Учёная степень не должна превышать 50 символов")
    @Column(name = "academic_degree", length = 50)
    private String academicDegree;

    @Email(message = "Некорректный формат email")
    @Size(max = 100, message = "Email не должен превышать 100 символов")
    @Column(name = "email", length = 100)
    private String email;

    @Size(max = 20, message = "Телефон не должен превышать 20 символов")
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}