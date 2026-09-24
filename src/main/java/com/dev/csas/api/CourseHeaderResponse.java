package com.dev.csas.api;

import com.dev.csas.course.CourseStatus;

public record CourseHeaderResponse(
        long id,
        String disciplineName,
        String groupName,
        String semesterName,
        CourseStatus status
) {
}
