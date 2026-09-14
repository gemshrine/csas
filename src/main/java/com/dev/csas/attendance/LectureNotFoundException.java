package com.dev.csas.attendance;

public class LectureNotFoundException extends RuntimeException{
    public LectureNotFoundException(String msg) {
        super(msg);
    }
}
