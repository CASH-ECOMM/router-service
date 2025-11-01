package com.cash.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class EndTimeResponseDto {
    private Instant endTime;
}
