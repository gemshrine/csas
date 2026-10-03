package com.dev.csas.api.course;

import com.dev.csas.course.CourseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CourseCreationRequest(
        @NotNull @Positive Long disciplineId,
        @NotNull @Positive Long groupId,
        @NotNull @Positive Long startSemesterId,
        @NotNull @Positive Long endSemesterId,
        @NotNull CourseType courseType,
        @NotNull @Positive Long teacherId
) {
}
