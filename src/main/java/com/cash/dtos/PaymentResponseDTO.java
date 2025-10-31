package com.cash.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment response to UI")
public class PaymentResponseDTO {

    @Schema(description = "Payment success status")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Payment ID", example = "pay-789")
    @JsonProperty("payment_id")
    private String paymentId;

    @Schema(description = "Response message")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Receipt information")
    @JsonProperty("receipt")
    private ReceiptDTO receipt;

    @Schema(description = "Shipping information message", example = "The item will be shipped in 5 days")
    @JsonProperty("shipping_message")
    private String shippingMessage;

    @Schema(description = "Transaction date", example = "2025-10-29T10:30:00")
    @JsonProperty("transaction_date")
    private String transactionDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiptDTO {

        @Schema(description = "Receipt ID")
        @JsonProperty("receipt_id")
        private String receiptId;

        @Schema(description = "Customer first name")
        @JsonProperty("first_name")
        private String firstName;

        @Schema(description = "Customer last name")
        @JsonProperty("last_name")
        private String lastName;

        @Schema(description = "Full delivery address")
        @JsonProperty("address")
        private String address;

        @Schema(description = "Item cost", example = "99")
        @JsonProperty("item_cost")
        private int itemCost;

        @Schema(description = "Shipping cost", example = "15")
        @JsonProperty("shipping_cost")
        private int shippingCost;

        @Schema(description = "HST amount (13%)", example = "14.95")
        @JsonProperty("hst_amount")
        private double hstAmount;

        @Schema(description = "Total amount paid", example = "129.94")
        @JsonProperty("total_paid")
        private double totalPaid;

        @Schema(description = "Item ID")
        @JsonProperty("item_id")
        private String itemId;
    }
}

