package com.pdfman.exception;

public class DatabaseConnectivityException extends RuntimeException {
    public DatabaseConnectivityException(String message) {
        super(message);
    }

    public DatabaseConnectivityException(String message, Throwable cause) {
        super(message, cause);
    }
}
