package com.dev.csas.coursework;

import com.dev.csas.student.StudentJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(
        name = "coursework_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coursework_results_sheet_student",
                columnNames = {"coursework_sheet_id", "student_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseworkResultJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ведомость обязательна")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coursework_sheet_id", nullable = false)
    private CourseworkSheetJPAEntity courseworkSheet;

    @NotNull(message = "Студент обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentJPAEntity student;

    @NotNull(message = "Балл обязателен")
    @Min(value = 0, message = "Балл должен быть от 0 до 100")
    @Max(value = 100, message = "Балл должен быть от 0 до 100")
    @Column(name = "score", nullable = false)
    private Integer score;
}
