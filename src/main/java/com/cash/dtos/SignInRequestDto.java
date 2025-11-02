package com.cash.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignInRequestDto {
  @NotBlank(message = "Username is required")
  private String username;

  @NotBlank(message = "Password is required")
  private String password;
}
