package com.cash.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "Request for estimating total cost (no credit card required)")
public class TotalCostRequestDTO {

    @NotNull @Positive
    @Schema(description = "Catalogue Item ID", example = "1001")
    @JsonProperty("item_id")
    private Integer itemId;

    @Schema(description = "Shipping type: REGULAR or EXPEDITED", example = "REGULAR")
    @JsonProperty("shipping_type")
    private com.cash.dtos.PaymentRequestDTO.ShippingTypeDTO shippingType;
}