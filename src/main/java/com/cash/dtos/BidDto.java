package com.cash.dtos;

import lombok.Data;
import java.time.Instant;

@Data
public class BidDto {
    private int bidId;
    private int userId;
    private int catalogueId;
    private int amount;
    private Instant timestamp;
}
