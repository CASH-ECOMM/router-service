package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class PlaceBidResponseDto extends RepresentationModel<PlaceBidResponseDto> {
  private boolean success;
  private String message;
  private int catalogueId;
  private int bidAmount;
}
