package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class AuctionStatusResponseDto extends RepresentationModel<AuctionStatusResponseDto> {
  private int highestBidder;
  private int currentHighestBid;
  private int remainingTime;
  private String auctionStatus;
  private int catalogueId;
}
