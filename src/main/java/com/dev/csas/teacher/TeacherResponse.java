package com.dev.csas.teacher;

public record TeacherResponse(
        Long id,
        String firstName,
        String lastName,
        String patronymic,
        String department,
        String position,
        String academicDegree,
        String email,
        String phone
) {
    public static TeacherResponse from(TeacherJPAEntity entity) {
        return new TeacherResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPatronymic(),
                entity.getDepartment(),
                entity.getPosition(),
                entity.getAcademicDegree(),
                entity.getEmail(),
                entity.getPhone()
        );
    }
}
