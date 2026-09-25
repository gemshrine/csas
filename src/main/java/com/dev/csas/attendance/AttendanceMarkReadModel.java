package com.dev.csas.attendance;

public record AttendanceMarkReadModel(long lectureId, long studentId, boolean present) {
}
