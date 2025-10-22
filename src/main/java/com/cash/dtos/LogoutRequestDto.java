package com.cash.dtos;

import lombok.Data;

@Data
public class LogoutRequestDto {
  private String jwt;
  private String userId;
}
