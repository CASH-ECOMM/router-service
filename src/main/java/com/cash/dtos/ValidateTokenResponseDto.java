package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ValidateTokenResponseDto extends RepresentationModel<ValidateTokenResponseDto> {
  private boolean valid;
  private int userId;
  private String username;
  private String role;
  private String message;
}
