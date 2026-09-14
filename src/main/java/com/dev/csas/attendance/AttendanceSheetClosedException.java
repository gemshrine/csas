package com.dev.csas.attendance;

public class AttendanceSheetClosedException extends RuntimeException {
    public AttendanceSheetClosedException(String msg) {
        super(msg);
    }
}
