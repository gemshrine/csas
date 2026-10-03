package com.dev.csas.coursework;

import com.dev.csas.course.CourseJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coursework_sheets")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseworkSheetJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Курс обязателен")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    private CourseJPAEntity course;

    @Builder.Default
    @NotNull(message = "Состояние ведомости обязательно")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseworkSheetStatus status = CourseworkSheetStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "courseworkSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseworkResultJPAEntity> results = new ArrayList<>();
}
