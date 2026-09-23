package com.dev.csas.course;

import com.dev.csas.student.StudentJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "course_works",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_works_course_number",
                columnNames = {"course_id", "number"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseWorkJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseJPAEntity course;

    @NotNull
    @Min(1)
    @Column(name = "number", nullable = false)
    private Long number;

    @NotBlank
    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Min(1)
    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @NotNull
    @Column(name = "work_date", nullable = false)
    private LocalDate date;

    @OneToMany(mappedBy = "work", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseScoreJPAEntity> scores = new ArrayList<>();
}
