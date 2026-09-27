package com.dev.csas.api.course;

import com.dev.csas.course.CourseStatus;

public record StudentCourseSummaryResponse(
        long courseId,
        String disciplineName,
        String groupName,
        String semesterName,
        CourseStatus status,
        double rating
) {
}
