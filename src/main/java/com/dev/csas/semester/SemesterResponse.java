package com.dev.csas.semester;

import java.time.LocalDate;

public record SemesterResponse(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate
) {
    public static SemesterResponse from(SemesterJPAEntity entity) {
        return new SemesterResponse(
                entity.getId(),
                entity.getName(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }
}
