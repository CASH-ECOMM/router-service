package com.cash.dtos;

import lombok.Data;

@Data
public class ForgotPasswordRequestDto {
  private String username;
  private String email;
}
