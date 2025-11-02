package com.cash.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LogoutRequestDto {
  @NotBlank(message = "JWT token is required")
  private String jwt;

  @Positive(message = "User ID must be positive")
  private int userId;
}
