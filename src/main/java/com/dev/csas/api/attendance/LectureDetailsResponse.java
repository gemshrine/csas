package com.dev.csas.api.attendance;

import java.time.LocalDate;
import java.util.List;

public record LectureDetailsResponse(long id, LocalDate date, String title, List<AttendanceStudentResponse> attendance) {
}
