package com.cash.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CatalogueItemRequestDto {
    private String title;
    private String description;
    private double startingPrice;
    private int durationHours;
    private int sellerId;
}
