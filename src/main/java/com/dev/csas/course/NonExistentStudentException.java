package com.dev.csas.course;

public class NonExistentStudentException extends RuntimeException {
    public NonExistentStudentException(String msg) {
        super(msg);
    }
}
