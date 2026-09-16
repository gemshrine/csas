package com.dev.csas.attendance;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lectures")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LectureJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ведомость посещаемости обязательна")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_sheet_id", nullable = false)
    private AttendanceSheetJPAEntity attendanceSheet;

    @NotNull(message = "Дата лекции обязательна")
    @Column(name = "lecture_date", nullable = false)
    private LocalDate lectureDate;

    @NotBlank(message = "Название лекции обязательно")
    @Size(max = 200, message = "Название лекции не должно превышать 200 символов")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
