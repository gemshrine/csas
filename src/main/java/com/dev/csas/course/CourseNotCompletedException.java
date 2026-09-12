package com.dev.csas.course;

public class CourseNotCompletedException extends RuntimeException {
    public CourseNotCompletedException(String message) {
        super(message);
    }
}
