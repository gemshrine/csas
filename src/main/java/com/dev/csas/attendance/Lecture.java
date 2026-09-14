package com.dev.csas.attendance;

import com.dev.csas.shared.LectureID;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class Lecture {
    private LectureID id;
    private LocalDate date;
    private String title;

    public Lecture(LectureID lectureID, LocalDate date, String title) {
        this.id = lectureID;
        this.date = date;
        this.title = title;
    }
}
