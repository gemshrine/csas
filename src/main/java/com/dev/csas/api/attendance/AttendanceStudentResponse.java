package com.dev.csas.api.attendance;

import com.dev.csas.attendance.AttendanceStatusEnum;

public record AttendanceStudentResponse(long studentId, String studentName, AttendanceStatusEnum status) {
}
