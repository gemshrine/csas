package com.dev.csas.group;

import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.teacher.TeacherJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "groups")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class GroupJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название группы обязательно")
    @Size(max = 50, message = "Название группы не должно превышать 50 символов")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // например, "ИВТ-41-1"

    @NotNull(message = "Год набора обязателен")
    @Min(value = 2000, message = "Год набора не может быть раньше 2000")
    @Max(value = 2100, message = "Год набора не может быть позже 2100")
    @Column(name = "year_of_admission", nullable = false)
    private Integer yearOfAdmission;

    @NotBlank(message = "Специальность обязательна")
    @Size(max = 100, message = "Специальность не должна превышать 100 символов")
    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    @NotNull(message = "Курс обязателен")
    @Min(value = 1, message = "Курс должен быть от 1 до 6")
    @Max(value = 6, message = "Курс должен быть от 1 до 6")
    @Column(name = "course", nullable = false)
    private Integer course;

    @NotNull(message = "Форма обучения обязательна")
    @Enumerated(EnumType.STRING)
    @Column(name = "education_form", nullable = false, length = 20)
    private EducationFormEnum educationForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curator_id")
    private TeacherJPAEntity curator;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}