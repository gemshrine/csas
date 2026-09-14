package com.dev.csas.attendance;

import com.dev.csas.shared.StudentID;

import java.time.LocalDate;

public record AttendanceRecord(
        AttendanceStatusEnum status
) {
}
