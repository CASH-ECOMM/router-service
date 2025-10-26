package com.cash.dtos;

import lombok.Data;

@Data
public class AddressDto {
  private String streetName;
  private String streetNumber;
  private String city;
  private String country;
  private String postalCode;
}
