package com.dev.csas.api.attendance;

import com.dev.csas.attendance.AttendanceStatusEnum;
import jakarta.validation.constraints.NotNull;

public record RegisterAttendanceRequest(@NotNull AttendanceStatusEnum status) {
}
