package com.cash.exceptions;

/**
 * Custom exception thrown when a requested resource is not found. Results in a
 * 404 HTTP response
 * when handled by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
