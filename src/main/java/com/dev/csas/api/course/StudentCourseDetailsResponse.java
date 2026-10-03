package com.dev.csas.api.course;

import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;
import com.dev.csas.coursework.CourseworkGrade;

import java.time.LocalDate;
import java.util.List;

public record StudentCourseDetailsResponse(
        long courseId,
        String disciplineName,
        String groupName,
        String startSemesterName,
        String endSemesterName,
        CourseType courseType,
        CourseStatus status,
        Double rating,
        Integer courseworkScore,
        CourseworkGrade courseworkGrade,
        StudentControlResultResponse controlResult,
        List<WorkResult> works
) {
    public record WorkResult(long number, String title, int maxScore, LocalDate date, Integer score) {
    }
}
