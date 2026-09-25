package com.dev.csas.api.attendance;

import java.time.LocalDate;

public record LectureResponse(long id, LocalDate date, String title) {
}
