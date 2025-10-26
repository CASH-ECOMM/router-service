package com.cash.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUserResponseDto {
  private boolean success;
  private int userId;
  private String username;
  private String firstName;
  private String lastName;
  private AddressDto shippingAddress;
  private String email;
  private String message;
}
