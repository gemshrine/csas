package com.dev.csas.api.course;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CourseReopenRequest(@NotNull @Positive Long semesterId) {
}
