package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class AuctionWinnerResponseDto extends RepresentationModel<AuctionWinnerResponseDto> {
  private boolean found;
  private int winningUserId;
  private int finalPrice;
  private String message;
  private int catalogueId;
}
