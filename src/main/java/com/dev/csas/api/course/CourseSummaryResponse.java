package com.dev.csas.api.course;

import com.dev.csas.attendance.AttendanceSheetStatus;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.CourseType;
import com.dev.csas.coursework.CourseworkSheetStatus;

public record CourseSummaryResponse(
        long courseId,
        String disciplineName,
        String groupName,
        String startSemesterName,
        String endSemesterName,
        CourseType courseType,
        CourseStatus courseStatus,
        Long controlSheetId,
        ControlSheetStatus controlSheetStatus,
        Long attendanceSheetId,
        AttendanceSheetStatus attendanceSheetStatus,
        Long courseworkSheetId,
        CourseworkSheetStatus courseworkSheetStatus,
        boolean canReadControlSheet,
        boolean canReadAttendanceSheet,
        boolean canReadCourseworkSheet
) {
}
