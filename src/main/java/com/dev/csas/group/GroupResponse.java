package com.dev.csas.group;

import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.enums.StudyFormEnum;

public record GroupResponse(
        Long id,
        String name,
        Integer yearOfAdmission,
        String specialty,
        Integer course,
        EducationFormEnum educationForm,
        StudyFormEnum studyForm
) {
    public static GroupResponse from(GroupJPAEntity entity) {
        return new GroupResponse(
                entity.getId(),
                entity.getName(),
                entity.getYearOfAdmission(),
                entity.getSpecialty(),
                entity.getCourse(),
                entity.getEducationForm(),
                entity.getStudyForm()
        );
    }
}
