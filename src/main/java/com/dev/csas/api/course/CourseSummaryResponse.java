package com.dev.csas.api.course;

import com.dev.csas.attendance.AttendanceSheetStatus;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.course.CourseStatus;

public record CourseSummaryResponse(
        long courseId,
        String disciplineName,
        String groupName,
        String semesterName,
        CourseStatus courseStatus,
        Long controlSheetId,
        ControlSheetStatus controlSheetStatus,
        Long attendanceSheetId,
        AttendanceSheetStatus attendanceSheetStatus,
        boolean canReadControlSheet,
        boolean canReadAttendanceSheet
) {
}
