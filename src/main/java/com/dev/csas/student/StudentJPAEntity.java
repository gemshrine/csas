package com.dev.csas.student;

import com.dev.csas.enums.GenderEnum;
import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.enums.StudentStatusEnum;
import com.dev.csas.user.UserJPAEntity;
import com.dev.csas.group.GroupJPAEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class StudentJPAEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "patronymic", length = 50)
    private String patronymic;

    @NotBlank(message = "Номер зачётки обязателен")
    @Size(min = 6, max = 10, message = "Номер зачётки должен содержать от 6 до 10 символов")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Номер зачётки может содержать только заглавные буквы и цифры")
    @Column(name = "record_book_number", unique = true, nullable = false, length = 10)
    private String recordBookNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Email(message = "Некорректный формат email")
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private GenderEnum gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_form", length = 20)
    private EducationFormEnum educationForm;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private StudentStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupJPAEntity group;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private UserJPAEntity user;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}