package com.cash.mappers;

import com.cash.dtos.*;
import com.cash.grpc.auctionservice.*;
import com.cash.grpc.catalogue.ItemResponse;
import com.google.protobuf.util.Timestamps;

import java.util.List;
import java.util.stream.Collectors;

public class AuctionServiceDtoMapper {

    public static StartAuctionRequest toProto(StartAuctionRequestDto dto) {
        return StartAuctionRequest.newBuilder()
            .setCatalogueId(dto.getCatalogueId())
            .setStartingAmount(dto.getStartingAmount())
            .setEndTime(Timestamps.fromMillis(dto.getEndTime().toEpochMilli()))
            .build();
    }

    public static PlaceBidRequest toProto(PlaceBidRequestDto dto) {
        return PlaceBidRequest.newBuilder()
            .setCatalogueId(dto.getCatalogueId())
            .setAmount(dto.getBidAmount())
            .build();
    }
}
