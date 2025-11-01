package com.cash.dtos;

import lombok.Data;

@Data
public class PlaceBidRequestDto {
    private int catalogueId;
    private int bidAmount;
}
