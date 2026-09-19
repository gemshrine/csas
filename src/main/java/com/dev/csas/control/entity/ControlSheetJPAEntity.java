package com.dev.csas.control.entity;

import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.course.CourseJPAEntity;
import com.dev.csas.discipline.ControlFormEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "control_sheets")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ControlSheetJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Курс обязателен")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    private CourseJPAEntity course;

    @NotNull(message = "Форма контроля обязательна")
    @Enumerated(EnumType.STRING)
    @Column(name = "control_form", nullable = false, length = 20)
    private ControlFormEnum controlForm;

    @Builder.Default
    @NotNull(message = "Состояние ведомости обязательно")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ControlSheetStatus status = ControlSheetStatus.OPEN;

    @NotNull(message = "Порог оценки «5» обязателен")
    @Min(value = 0, message = "Порог оценки «5» должен быть от 0 до 100")
    @Max(value = 100, message = "Порог оценки «5» должен быть от 0 до 100")
    @Column(name = "grade_5_from", nullable = false)
    private Integer grade5From;

    @NotNull(message = "Порог оценки «4» обязателен")
    @Min(value = 0, message = "Порог оценки «4» должен быть от 0 до 100")
    @Max(value = 100, message = "Порог оценки «4» должен быть от 0 до 100")
    @Column(name = "grade_4_from", nullable = false)
    private Integer grade4From;

    @NotNull(message = "Порог оценки «3» обязателен")
    @Min(value = 0, message = "Порог оценки «3» должен быть от 0 до 100")
    @Max(value = 100, message = "Порог оценки «3» должен быть от 0 до 100")
    @Column(name = "grade_3_from", nullable = false)
    private Integer grade3From;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
