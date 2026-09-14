package com.dev.csas.attendance;

public class DuplicateLectureException extends RuntimeException {
    public DuplicateLectureException(String msg) {
        super(msg);
    }
}
