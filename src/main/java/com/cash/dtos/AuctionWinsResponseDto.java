package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class AuctionWinsResponseDto extends RepresentationModel<AuctionWinsResponseDto> {
    private int finalPrice;
    private int catalogueId;
    private String itemName;
}
