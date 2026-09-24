package com.dev.csas.api.attendance;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.attendance.AttendanceSheetStatus;

import java.util.List;

public record AttendanceSheetDetailsResponse(
        long id,
        CourseHeaderResponse course,
        AttendanceSheetStatus status,
        List<AttendanceRosterStudentResponse> students,
        List<LectureDetailsResponse> lectures
) {
}
