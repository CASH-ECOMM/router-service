package com.cash.dtos;

import lombok.Data;

@Data
public class SignUpRequestDto {
  private String username;
  private String password;
  private String firstName;
  private String lastName;
  private AddressDto shippingAddress;
  private String email;
}
