package com.dev.csas.attendance;

public record AttendanceSheetReadModel(long courseId, long sheetId, AttendanceSheetStatus status) {
}
