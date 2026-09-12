package com.dev.csas.control;

public record GradingScale(int grade5From, int grade4From, int grade3From) {

    public GradingScale {
        if (grade5From < 0 || grade4From < 0 || grade3From < 0
                || grade5From > 100 || grade4From > 100 || grade3From > 100
                || grade5From < grade4From || grade4From < grade3From) {
            throw new IllegalArgumentException("Пороговые значения шкалы некорректны");
        }
    }

    public int grade(double score) {
        int roundedDown = (int) Math.floor(score);
        if (roundedDown >= grade5From) {
            return 5;
        }
        if (roundedDown >= grade4From) {
            return 4;
        }
        if (roundedDown >= grade3From) {
            return 3;
        }
        return 2;
    }
}
