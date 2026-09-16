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

@Entity
@Table(
        name = "courses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_courses_discipline_group_semester",
                columnNames = {"discipline_id", "group_id", "semester_id"}
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

    @NotNull(message = "Семестр обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private SemesterJPAEntity semester;

    @NotNull(message = "Преподаватель семинарной части обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seminar_teacher_id", nullable = false)
    private TeacherJPAEntity seminarTeacher;

    @NotNull(message = "Преподаватель лекционной части обязателен")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_teacher_id", nullable = false)
    private TeacherJPAEntity lectureTeacher;

    @Builder.Default
    @NotNull(message = "Состояние курса обязательно")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status = CourseStatus.IN_PROGRESS;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
