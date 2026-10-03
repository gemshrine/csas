package com.dev.csas.coursework;

import com.dev.csas.api.CourseHeaderResponse;

import java.util.List;

public record CourseworkSheetDetailsResponse(
        long sheetId,
        CourseHeaderResponse course,
        CourseworkSheetStatus status,
        List<StudentResult> results
) {
    public record StudentResult(long studentId, String studentName, Integer score, CourseworkGrade grade) {
    }
}
