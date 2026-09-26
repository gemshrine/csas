package com.dev.csas.student;

import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.enums.GenderEnum;
import com.dev.csas.enums.StudentStatusEnum;
import com.dev.csas.enums.StudyFormEnum;
import com.dev.csas.group.GroupJPAEntity;

import java.time.LocalDate;

public record StudentResponse(
        Long id,
        String firstName,
        String lastName,
        String patronymic,
        String recordBookNumber,
        LocalDate birthDate,
        String email,
        String phone,
        GenderEnum gender,
        EducationFormEnum educationForm,
        StudyFormEnum studyForm,
        LocalDate admissionDate,
        StudentStatusEnum status,
        GroupRef group
) {
    public record GroupRef(Long id, String name) {
    }

    public static StudentResponse from(StudentJPAEntity entity) {
        GroupJPAEntity group = entity.getGroup();
        GroupRef groupRef = group == null ? null : new GroupRef(group.getId(), group.getName());
        return new StudentResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPatronymic(),
                entity.getRecordBookNumber(),
                entity.getBirthDate(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getGender(),
                entity.getEducationForm(),
                entity.getStudyForm(),
                entity.getAdmissionDate(),
                entity.getStatus(),
                groupRef
        );
    }
}
