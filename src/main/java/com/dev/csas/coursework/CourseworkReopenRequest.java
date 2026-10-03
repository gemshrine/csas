package com.dev.csas.coursework;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CourseworkReopenRequest(@NotNull @Positive Long semesterId) {
}
