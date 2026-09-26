package com.dev.csas.api.course;

import com.dev.csas.course.CourseStatus;

import java.time.LocalDate;
import java.util.List;

public record CourseDetailsResponse(
        long courseId,
        String disciplineName,
        String groupName,
        String semesterName,
        CourseStatus status,
        List<CourseWorkResponse> works,
        List<CourseStudentResponse> students
) {
    public record CourseWorkResponse(long number, String title, int maxScore, LocalDate date,
                                     List<CourseWorkScoreResponse> scores) {
    }

    public record CourseWorkScoreResponse(long studentId, int score) {
    }

    public record CourseStudentResponse(long studentId, String name, double rating) {
    }
}
