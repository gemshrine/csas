package com.dev.csas.course;

import com.dev.csas.student.StudentJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(
        name = "course_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_scores_work_student",
                columnNames = {"work_id", "student_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseScoreJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false)
    private CourseWorkJPAEntity work;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentJPAEntity student;

    @NotNull
    @Min(0)
    @Column(name = "score", nullable = false)
    private Integer score;
}
