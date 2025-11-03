package com.cash.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Standard error response DTO for all API error responses. Contains only essential information:
 * HTTP status code and error message.
 */
@Data
@Builder
@Schema(description = "Standard error response")
public class ErrorResponse {

  @Schema(description = "HTTP status code", example = "404")
  private int status;

  @Schema(description = "Error message describing what went wrong", example = "Resource not found")
  private String message;

  /**
   * Convenience factory method to create an ErrorResponse.
   *
   * @param status HTTP status code
   * @param message Error message
   * @return ErrorResponse instance
   */
  public static ErrorResponse of(int status, String message) {
    return ErrorResponse.builder().status(status).message(message).build();
  }
}
