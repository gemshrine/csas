package com.dev.csas.backup;

public class DatabaseBackupException extends RuntimeException {
    public DatabaseBackupException(String message, Throwable cause) {
        super(message, cause);
    }
}
