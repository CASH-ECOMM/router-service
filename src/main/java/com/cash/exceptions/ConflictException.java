package com.cash.exceptions;

/**
 * Custom exception thrown when a request conflicts with the current state of
 * the resource.
 * Results in a 409 HTTP response when handled by GlobalExceptionHandler.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
