package com.cash.config;

import com.cash.dtos.ErrorResponse;
import com.cash.exceptions.ConflictException;
import com.cash.exceptions.ResourceNotFoundException;
import com.cash.exceptions.UnauthorizedException;
import io.grpc.StatusRuntimeException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for all controllers. Provides consistent error
 * response format across
 * the entire API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException. Returns 404 Not Found with error message.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles UnauthorizedException. Returns 401 Unauthorized with error message.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        ErrorResponse error = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles ConflictException. Returns 409 Conflict with error message.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        ErrorResponse error = ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles gRPC StatusRuntimeException from downstream services. Maps gRPC
     * status codes to
     * appropriate HTTP status codes.
     */
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcException(StatusRuntimeException ex) {
        HttpStatus status = mapGrpcStatusToHttp(ex);

        String errorMessage = ex.getStatus().getDescription();
        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = "Service error occurred";
        }

        ErrorResponse error = ErrorResponse.of(status.value(), errorMessage);
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handles validation errors from @Valid annotations. Returns 400 Bad Request
     * with validation
     * error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        // Collect all field validation errors
        String validationErrors = ex.getBindingResult().getAllErrors().stream()
                .map(
                        error -> {
                            String fieldName = ((FieldError) error).getField();
                            String errorMessage = error.getDefaultMessage();
                            return fieldName + ": " + errorMessage;
                        })
                .collect(Collectors.joining(", "));

        ErrorResponse error = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                "Validation failed: " + validationErrors);

        return ResponseEntity.badRequest().body(error);
    }

    /** Handles IllegalArgumentException. Returns 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    /** Handles all other uncaught exceptions. Returns 500 Internal Server Error. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Log the exception for debugging (in production, use proper logging)
        ex.printStackTrace();

        ErrorResponse error = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /** Maps gRPC status codes to HTTP status codes. */
    private HttpStatus mapGrpcStatusToHttp(StatusRuntimeException e) {
        return switch (e.getStatus().getCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case FAILED_PRECONDITION -> HttpStatus.PRECONDITION_FAILED;
            case ABORTED -> HttpStatus.CONFLICT;
            case OUT_OF_RANGE -> HttpStatus.BAD_REQUEST;
            case UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
