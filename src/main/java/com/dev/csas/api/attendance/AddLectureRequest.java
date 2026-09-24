package com.dev.csas.api.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AddLectureRequest(
        @NotNull LocalDate date,
        @NotBlank @Size(max = 200) String title
) {
}
