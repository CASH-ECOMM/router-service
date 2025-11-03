package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ErrorResponseDto extends RepresentationModel<ErrorResponseDto> {
  private String message;
  private String error;
  private int status;
}
