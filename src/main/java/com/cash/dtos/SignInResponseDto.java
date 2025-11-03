package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class SignInResponseDto extends RepresentationModel<SignInResponseDto> {
  private String jwt;
  private int userId;
  private String message;
}
