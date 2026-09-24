package com.dev.csas.attendance;

public class AttendanceSheetNotFoundException extends RuntimeException {
    public AttendanceSheetNotFoundException(String message) {
        super(message);
    }
}
