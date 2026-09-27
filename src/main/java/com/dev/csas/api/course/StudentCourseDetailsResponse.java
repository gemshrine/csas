package com.dev.csas.api.course;

import com.dev.csas.course.CourseStatus;

import java.time.LocalDate;
import java.util.List;

public record StudentCourseDetailsResponse(
        long courseId,
        String disciplineName,
        String groupName,
        String semesterName,
        CourseStatus status,
        double rating,
        List<WorkResult> works
) {
    public record WorkResult(long number, String title, int maxScore, LocalDate date, Integer score) {
    }
}
