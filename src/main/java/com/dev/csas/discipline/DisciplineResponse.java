package com.dev.csas.discipline;

public record DisciplineResponse(
        Long id,
        String name,
        String description,
        Integer lectureHours,
        Integer practiceHours,
        Integer labHours,
        ControlFormEnum controlForm
) {
    public static DisciplineResponse from(DisciplineJPAEntity entity) {
        return new DisciplineResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getLectureHours(),
                entity.getPracticeHours(),
                entity.getLabHours(),
                entity.getControlForm()
        );
    }
}
