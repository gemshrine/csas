package com.dev.csas.api.course;

import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;
import com.dev.csas.coursework.CourseworkGrade;

public record StudentCourseSummaryResponse(
        long courseId,
        String disciplineName,
        String groupName,
        String startSemesterName,
        String endSemesterName,
        CourseType courseType,
        CourseStatus status,
        Double rating,
        Integer courseworkScore,
        CourseworkGrade courseworkGrade
) {
}
