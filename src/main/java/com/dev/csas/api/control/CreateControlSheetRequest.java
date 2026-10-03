package com.dev.csas.api.control;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

public record CreateControlSheetRequest(
        @NotNull @Min(0) @Max(100) Integer grade5From,
        @NotNull @Min(0) @Max(100) Integer grade4From,
        @NotNull @Min(0) @Max(100) Integer grade3From
) {
    @AssertTrue(message = "Пороги оценок должны идти по убыванию")
    public boolean isScaleOrdered() {
        return grade5From == null || grade4From == null || grade3From == null
                || grade5From >= grade4From && grade4From >= grade3From;
    }
}
