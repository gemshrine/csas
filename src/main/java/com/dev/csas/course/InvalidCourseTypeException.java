package com.dev.csas.course;

public class InvalidCourseTypeException extends RuntimeException {
    public InvalidCourseTypeException(String message) {
        super(message);
    }
}
