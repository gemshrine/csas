package com.dev.csas.course;

import com.dev.csas.shared.StudentID;

public class Score {
    private long workNumber;
    private StudentID studentId;
    private int score;

    public Score(long workNumber, StudentID studentId, int score) {
        this.workNumber = workNumber;
        this.studentId = studentId;
        this.score = score;
    }

    void setScore(int score) {
        this.score = score;
    }

    long getWorkNumber() {
        return workNumber;
    }

    StudentID getStudentId() {
        return studentId;
    }

    int getScore() {
        return score;
    }
}
