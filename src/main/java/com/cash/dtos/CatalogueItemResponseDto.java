package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class CatalogueItemResponseDto extends RepresentationModel<CatalogueItemResponseDto> {
    private int id;
    private String title;
    private String description;
    private int startingPrice;
    private int currentPrice;
    private boolean active;
    private int durationHours;
    private String createdAt;
    private String endTime;
    private int sellerId;
    private int remainingTimeSeconds;
    private int shippingCost;
    private int shippingTime;
    private String message;
    private boolean success;
}