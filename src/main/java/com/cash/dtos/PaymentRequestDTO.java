package com.cash.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment request from UI")
public class PaymentRequestDTO {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    @Schema(description = "User ID", example = "101")
    @JsonProperty("user_id")
    private Integer userId;

    @NotNull(message = "Item ID is required")
    @Positive(message = "Item ID must be positive")
    @Schema(description = "Item ID from catalogue", example = "1001")
    @JsonProperty("item_id")
    private Integer itemId;

    @NotNull(message = "Item cost is required")
    @Min(value = 0, message = "Item cost must be >= 0")
    @Schema(description = "Item cost in whole dollars", example = "100")
    @JsonProperty("item_cost")
    private Integer itemCost;

    @NotNull(message = "Shipping cost is required")
    @Min(value = 0, message = "Shipping cost must be >= 0")
    @Schema(description = "Base shipping cost in whole dollars (server applies surcharge for EXPEDITED)", example = "15")
    @JsonProperty("shipping_cost")
    private Integer shippingCost;

    @NotNull(message = "Estimated shipping days is required")
    @Min(value = 0, message = "Estimated days must be >= 0")
    @Schema(description = "Estimated delivery days", example = "3")
    @JsonProperty("estimated_days")
    private Integer estimatedDays;

    @NotNull(message = "Shipping type is required")
    @Schema(description = "Shipping type: REGULAR or EXPEDITED", example = "REGULAR")
    @JsonProperty("shipping_type")
    private ShippingTypeDTO shippingType;

    @Schema(description = "Customer first name", example = "John")
    @JsonProperty("first_name")
    private String firstName;

    @Schema(description = "Customer last name", example = "Doe")
    @JsonProperty("last_name")
    private String lastName;

    @Schema(description = "Street name", example = "Main Street")
    @JsonProperty("street")
    private String street;

    @Schema(description = "Province", example = "Ontario")
    @JsonProperty("province")
    private String province;

    @Schema(description = "Country", example = "Canada")
    @JsonProperty("country")
    private String country;

    @Schema(description = "Postal code", example = "M5H 2N2")
    @JsonProperty("postal_code")
    private String postalCode;

    @Schema(description = "Street number (string in proto)", example = "123")
    @JsonProperty("number")
    private String number;

    @NotNull(message = "Credit card information is required")
    @Schema(description = "Credit card information")
    @JsonProperty("credit_card")
    private CreditCardDTO creditCard;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreditCardDTO {

        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9]{13,19}$", message = "Invalid card number format")
        @Schema(description = "Credit card number (13-19 digits)", example = "4532015112830366")
        @JsonProperty("card_number")
        private String cardNumber;

        @NotBlank(message = "Name on card is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        @Schema(description = "Name on card", example = "John Doe")
        @JsonProperty("name_on_card")
        private String nameOnCard;

        @NotBlank(message = "Expiry date is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Expiry date must be in MM/YY format")
        @Schema(description = "Card expiry date (MM/YY)", example = "12/25")
        @JsonProperty("expiry_date")
        private String expiryDate;

        @NotBlank(message = "Security code is required")
        @Pattern(regexp = "^[0-9]{3,4}$", message = "Security code must be 3 or 4 digits")
        @Schema(description = "Card security code (CVV/CVC)", example = "123")
        @JsonProperty("security_code")
        private String securityCode;
    }

    public enum ShippingTypeDTO {
        REGULAR,
        EXPEDITED
    }
}


