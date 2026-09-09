package com.dev.csas.course;

import java.time.LocalDate;

public class Work {

    private final long number;
    private String title;
    private int maxScore;
    private LocalDate date;

    public Work(long number, String title, int maxScore, LocalDate date) {
        this.number = number;
        this.title = title;
        this.maxScore = maxScore;
        this.date = date;
    }

    long getNumber() {
        return number;
    }

    int getMaxScore() {
        return maxScore;
    }

    LocalDate getDate() {
        return date;
    }

    void updateMaxScore(int newMaxScore) {
        if (this.maxScore == newMaxScore) {
            return;
        }

        this.maxScore = newMaxScore;
    }

    void updateTitle(String newTitle) {
        if (this.title.equals(newTitle)) {
            return;
        }

        this.title = newTitle;
    }

    void updateDate(LocalDate newDate) {
        if (this.date.isEqual(newDate)) {
            return;
        }

        this.date = newDate;
    }
}
