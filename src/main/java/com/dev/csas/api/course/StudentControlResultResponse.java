package com.dev.csas.api.course;

import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.control.PassFail;
import com.dev.csas.discipline.ControlFormEnum;

public record StudentControlResultResponse(
        ControlFormEnum controlForm,
        ControlSheetStatus sheetStatus,
        Integer numericScore,
        PassFail passFail,
        Double finalScore,
        Integer finalGrade
) {
}
