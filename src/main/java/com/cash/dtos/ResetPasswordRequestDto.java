package com.cash.dtos;

import lombok.Data;

@Data
public class ResetPasswordRequestDto {
  private String token;
  private String newPassword;
}
