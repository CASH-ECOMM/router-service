package com.cash.mappers;

import com.cash.dtos.CatalogueItemRequestDto;
import com.cash.dtos.CatalogueItemResponseDto;
import com.cash.grpc.catalogue.CreateItemRequest;
import com.cash.grpc.catalogue.ItemResponse;


import java.util.List;
import java.util.stream.Collectors;

public class CatalogueServiceDtoMapper {

    public static CreateItemRequest toProto(CatalogueItemRequestDto dto) {
        return CreateItemRequest.newBuilder()
                .setTitle(dto.getTitle())
                .setDescription(dto.getDescription())
                .setStartingPrice((int)dto.getStartingPrice())
                .setDurationHours(dto.getDurationHours())
                .setSellerId(dto.getSellerId())
                .build();
    }

    public static CatalogueItemResponseDto fromProto(ItemResponse response) {
        return CatalogueItemResponseDto.builder()
                .id(response.getId())
                .title(response.getTitle())
                .description(response.getDescription())
                .startingPrice(response.getStartingPrice())
                .currentPrice(response.getCurrentPrice())
                .active(response.getActive())
                .durationHours(response.getDurationHours())
                .createdAt(response.getCreatedAt())
                .endTime(response.getEndTime())
                .sellerId(response.getSellerId())
                .remainingTimeSeconds(response.getRemainingTimeSeconds())
                .shippingCost(response.getShippingCost())
                .shippingTime(response.getShippingTime())
                .message("Item created successfully") 
                .success(true) 
                .build();
    }

    public static List<CatalogueItemResponseDto> fromProtoList(List<ItemResponse> items) {
        return items.stream()
                .map(CatalogueServiceDtoMapper::fromProto)
                .collect(Collectors.toList());
    }
}
