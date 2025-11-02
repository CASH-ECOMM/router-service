package com.cash.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddressDto {
  @NotBlank(message = "Street name is required")
  private String streetName;

  @NotBlank(message = "Street number is required")
  private String streetNumber;

  @NotBlank(message = "City is required")
  private String city;

  @NotBlank(message = "Country is required")
  private String country;

  @NotBlank(message = "Postal code is required")
  @Pattern(regexp = "^[A-Z]\\d[A-Z] \\d[A-Z]\\d$", message = "Postal code must be in format: L5M 7J3")
  private String postalCode;
}
