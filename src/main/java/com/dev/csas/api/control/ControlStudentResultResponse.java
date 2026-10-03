package com.dev.csas.api.control;

import com.dev.csas.control.PassFail;

public record ControlStudentResultResponse(
        long studentId,
        String studentName,
        Integer numericScore,
        PassFail passFail,
        Double finalScore,
        Integer finalGrade
) {
}
