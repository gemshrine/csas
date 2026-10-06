package com.dev.csas.api.course;

import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;

import java.time.LocalDate;
import java.util.List;

public record CourseDetailsResponse(
        long courseId,
        Long disciplineId,
        String disciplineName,
        Long groupId,
        String groupName,
        Long startSemesterId,
        String startSemesterName,
        Long endSemesterId,
        String endSemesterName,
        Long teacherId,
        CourseType courseType,
        CourseStatus status,
        List<CourseWorkResponse> works,
        List<CourseStudentResponse> students
) {
    public CourseDetailsResponse(
            long courseId,
            String disciplineName,
            String groupName,
            String startSemesterName,
            String endSemesterName,
            CourseType courseType,
            CourseStatus status,
            List<CourseWorkResponse> works,
            List<CourseStudentResponse> students
    ) {
        this(courseId, null, disciplineName, null, groupName, null, startSemesterName,
                null, endSemesterName, null, courseType, status, works, students);
    }

    public record CourseWorkResponse(long number, String title, int maxScore, LocalDate date,
                                     List<CourseWorkScoreResponse> scores) {
    }

    public record CourseWorkScoreResponse(long studentId, int score) {
    }

    public record CourseStudentResponse(long studentId, String name, Double rating) {
    }
}
