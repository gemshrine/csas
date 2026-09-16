package com.dev.csas.control;

import com.dev.csas.student.StudentJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "control_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_control_results_sheet_student",
                columnNames = {"control_sheet_id", "student_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ControlResultJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Зачётная ведомость обязательна")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_sheet_id", nullable = false)
    private ControlSheetJPAEntity controlSheet;

    @NotNull(message = "Студент обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentJPAEntity student;

    @Min(value = 0, message = "Результат контроля должен быть от 0 до 100")
    @Max(value = 100, message = "Результат контроля должен быть от 0 до 100")
    @Column(name = "numeric_score")
    private Integer numericScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_fail", length = 10)
    private PassFail passFail;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
