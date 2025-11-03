package com.cash.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GetUserResponseDto extends RepresentationModel<GetUserResponseDto> {
  private boolean success;
  private int userId;
  private String username;
  private String firstName;
  private String lastName;
  private AddressDto shippingAddress;
  private String email;
  private String message;
}
