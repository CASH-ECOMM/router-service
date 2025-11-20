package com.cash.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuctionWinsResponseDto {
    private int finalPrice;
    private int catalogueId;
}
