package com.dev.csas.coursework;

public enum CourseworkGrade {
    FIVE,
    FOUR,
    THREE,
    NOT_PASSED;

    public static CourseworkGrade fromScore(int score) {
        if (score < 0 || score > 100) {
            throw new InvalidCourseworkScoreException("Балл за курсовую работу должен быть от 0 до 100");
        }
        if (score >= 85) {
            return FIVE;
        }
        if (score >= 70) {
            return FOUR;
        }
        if (score >= 50) {
            return THREE;
        }
        return NOT_PASSED;
    }
}
