package com.cash.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CatalogueItemRequestDto {

    private String title;
    private String description;
    private int startingPrice;
    private int durationHours;

    
}
