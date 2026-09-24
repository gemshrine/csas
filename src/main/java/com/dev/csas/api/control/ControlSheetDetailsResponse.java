package com.dev.csas.api.control;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.discipline.ControlFormEnum;

import java.util.List;

public record ControlSheetDetailsResponse(
        long id,
        CourseHeaderResponse course,
        ControlFormEnum controlForm,
        ControlSheetStatus status,
        GradingScaleResponse gradingScale,
        List<ControlStudentResultResponse> students
) {
}
