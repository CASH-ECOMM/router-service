package com.cash.dtos;

import lombok.Data;

@Data
public class SignInRequestDto {
  private String username;
  private String password;
}
