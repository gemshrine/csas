package com.dev.csas.discipline;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "disciplines")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DisciplineJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название дисциплины обязательно")
    @Size(max = 100, message = "Название не должно превышать 100 символов")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Количество часов лекций обязательно")
    @Min(value = 0, message = "Часы лекций не могут быть отрицательными")
    @Column(name = "lecture_hours", nullable = false)
    private Integer lectureHours;

    @NotNull(message = "Количество часов практик обязательно")
    @Min(value = 0, message = "Часы практик не могут быть отрицательными")
    @Column(name = "practice_hours", nullable = false)
    private Integer practiceHours;

    @NotNull(message = "Количество часов лабораторных обязательно")
    @Min(value = 0, message = "Часы лабораторных не могут быть отрицательными")
    @Column(name = "lab_hours", nullable = false)
    private Integer labHours;

    // Форма контроля: EXAM, DIFF_CREDIT, CREDIT
    @NotBlank(message = "Форма контроля обязательна")
    @Size(max = 20, message = "Форма контроля не должна превышать 20 символов")
    @Column(name = "control_form", nullable = false, length = 20)
    private String controlForm;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}