package com.dev.csas.course;

import com.dev.csas.discipline.DisciplineJPAEntity;
import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.semester.SemesterJPAEntity;
import com.dev.csas.teacher.TeacherJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.dev.csas.student.StudentJPAEntity;

@Entity
@Table(
        name = "courses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_courses_discipline_group_period_type",
                columnNames = {
                        "discipline_id", "group_id", "start_semester_id", "end_semester_id", "course_type"
                }
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Дисциплина обязательна")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipline_id", nullable = false)
    private DisciplineJPAEntity discipline;

    @NotNull(message = "Группа обязательна")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupJPAEntity group;

    @NotNull(message = "Семестр начала обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "start_semester_id", nullable = false)
    private SemesterJPAEntity startSemester;

    @NotNull(message = "Семестр окончания обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "end_semester_id", nullable = false)
    private SemesterJPAEntity endSemester;

    @NotNull(message = "Тип курса обязателен")
    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 20)
    private CourseType courseType;

    @NotNull(message = "Преподаватель курса обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherJPAEntity teacher;

    @Builder.Default
    @NotNull(message = "Состояние курса обязательно")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status = CourseStatus.IN_PROGRESS;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    @Builder.Default
    private List<CourseWorkJPAEntity> works = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_students",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_course_students_course_student",
                    columnNames = {"course_id", "student_id"}
            )
    )
    @Builder.Default
    private List<StudentJPAEntity> students = new ArrayList<>();
}
