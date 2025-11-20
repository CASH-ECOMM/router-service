package com.cash.mappers;

import com.cash.dtos.AuctionWinnerResponseDto;
import com.cash.dtos.AuctionWinsResponseDto;
import com.cash.dtos.EndTimeResponseDto;
import com.cash.dtos.PlaceBidRequestDto;
import com.cash.grpc.auctionservice.*;
import com.cash.grpc.catalogue.ItemResponse;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionServiceDtoMapper {

    public static PlaceBidRequest toProto(PlaceBidRequestDto dto) {
        return PlaceBidRequest.newBuilder()
            .setAmount(dto.getBidAmount())
            .build();
    }

    public static EndTimeResponseDto fromProto(GetAuctionEndResponse response) {
        return EndTimeResponseDto.builder()
                .endTime(Instant.ofEpochSecond(response.getEndTime().getSeconds(), response.getEndTime().getNanos()))
                .build();
    }

    public static List<AuctionWinsResponseDto> fromProtoList(List<AuctionWinnerResponseDto> items) {
        return items.stream()
                .map(item -> AuctionWinsResponseDto.builder()
                        .finalPrice(item.getFinalPrice())
                        .catalogueId(item.getCatalogueId())
                        .build())
                .collect(Collectors.toList());
    }
}
