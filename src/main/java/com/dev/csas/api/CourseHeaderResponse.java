package com.dev.csas.api;

import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;

public record CourseHeaderResponse(
        long id,
        String disciplineName,
        String groupName,
        String startSemesterName,
        String endSemesterName,
        CourseType courseType,
        CourseStatus status
) {
}
