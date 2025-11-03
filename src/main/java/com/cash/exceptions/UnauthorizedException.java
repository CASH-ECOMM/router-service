package com.cash.exceptions;

/**
 * Custom exception thrown when authentication fails or is required. Results in
 * a 401 HTTP response
 * when handled by GlobalExceptionHandler.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
