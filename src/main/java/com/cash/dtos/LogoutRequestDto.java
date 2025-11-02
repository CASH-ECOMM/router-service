package com.cash.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LogoutRequestDto {
  @NotBlank(message = "JWT token is required")
  private String jwt;

  @NotNull(message = "User ID is required")
  @Positive(message = "User ID must be positive")
  private Integer userId;
}
