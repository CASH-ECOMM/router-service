package com.cash.dtos;

import lombok.Data;
import java.time.Instant;

@Data
public class StartAuctionRequestDto {
    private int catalogueId;
    private int startingAmount;
    private Instant endTime;
}
