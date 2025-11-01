package com.cash.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TotalCostDTO {
    @JsonProperty("item_cost")  @Schema(example = "100")  private int itemCost;
    @JsonProperty("hst_rate")   @Schema(example = "0.13") private double hstRate;
    @JsonProperty("hst_amount") @Schema(example = "14.95")private double hstAmount;
    @JsonProperty("total_cost") @Schema(example = "129.95")private double totalCost;
    @JsonProperty("message")    @Schema(example = "Total cost calculated") private String message;
}
