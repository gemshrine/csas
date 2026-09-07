package com.dev.csas.course;

public class Work {

    private final long number;
    private String title;
    private int maxScore;

    public Work(long number, String title, int maxScore) {
        this.number = number;
        this.title = title;
        this.maxScore = maxScore;
    }

    long getNumber() {
        return number;
    }

    int getMaxScore() {
        return maxScore;
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
}
