package com.cash.mappers;

import com.cash.dtos.EndTimeResponseDto;
import com.cash.dtos.PlaceBidRequestDto;
import com.cash.grpc.auctionservice.*;
import java.time.Instant;

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
}
