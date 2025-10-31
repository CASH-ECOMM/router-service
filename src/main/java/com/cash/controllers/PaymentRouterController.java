package com.cash.controllers;

import com.ecommerce.payment.grpc.*;
import com.cash.dtos.PaymentRequestDTO;
import com.cash.dtos.PaymentResponseDTO;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.devh.boot.grpc.client.inject.GrpcClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Payment Router", description = "Payment routing endpoints for payment processing")
public class PaymentRouterController {


    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub;


//    @Qualifier("userServiceChannel")
//    private final ManagedChannel userServiceChannel;
//
//    @Qualifier("catalogueServiceChannel")
//    private final ManagedChannel catalogueServiceChannel;

    /**
     * Use Case 5: Process Payment
     * Receives payment request from UI, aggregates data from other services,
     * and forwards to Payment Service
     */
    @PostMapping("/process")
    @Operation(
            summary = "Process payment",
            description = "Process payment by aggregating user info, catalogue data, and credit card information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid payment request"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO request) {

        log.info("Received payment request for user: {} and item: {}",
                request.getUserId(), request.getItemId());

        try {
            // Get user information from User Service (mock for now)
            UserInfo userInfo = getUserInfoFromUserService(request.getUserId());

            // Get item details from Catalogue Service (mock for now)
            ItemDetails itemDetails = getItemDetailsFromCatalogueService(request.getItemId());

            // Calculate shipping cost based on type
            int shippingCost = calculateShippingCost(request.getShippingType(), itemDetails.getBaseShippingCost());

            // Build gRPC payment request
            PaymentRequest grpcRequest = buildGrpcPaymentRequest(request, userInfo, itemDetails, shippingCost);

            // Call Payment Service via gRPC
            PaymentResponse grpcResponse = paymentStub.processPayment(grpcRequest);


            // Convert gRPC response to REST DTO
            PaymentResponseDTO responseDTO = convertToDTO(grpcResponse);

            log.info("Payment processed successfully with ID: {}", responseDTO.getPaymentId());

            return ResponseEntity.ok(responseDTO);

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while processing payment", e);
            PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
                    .success(false)
                    .message("Payment service error: " + e.getStatus().getDescription())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } catch (Exception e) {
            log.error("Error processing payment", e);
            PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
                    .success(false)
                    .message("An error occurred: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Use Case 6: Get Payment Receipt
     * Retrieve payment details and receipt information
     */
    @GetMapping("/{paymentId}")
    @Operation(
            summary = "Get payment by ID",
            description = "Retrieve payment details and receipt information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment found",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found"
            )
    })
    public ResponseEntity<PaymentResponseDTO> getPayment(@PathVariable String paymentId) {
        log.info("Retrieving payment with ID: {}", paymentId);

        try {
            GetPaymentRequest grpcRequest = GetPaymentRequest.newBuilder().setPaymentId(paymentId).build();
            PaymentResponse grpcResponse = paymentStub.getPaymentById(grpcRequest);
            if (!grpcResponse.getSuccess()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(convertToDTO(grpcResponse));
            }

            return ResponseEntity.ok(convertToDTO(grpcResponse));

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while retrieving payment", e);
            PaymentResponseDTO errorResponse = PaymentResponseDTO.builder()
                    .success(false)
                    .message("Payment not found")
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Get Payment History
     */
    @GetMapping("/history/{userId}")
    @Operation(
            summary = "Get payment history",
            description = "Retrieve payment history for a user"
    )
    public ResponseEntity<?> getPaymentHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Retrieving payment history for user: {}", userId);

        try {
            PaymentHistoryRequest grpcRequest = PaymentHistoryRequest.newBuilder()
                    .setUserId(userId)
                    .setPage(page)
                    .setSize(size)
                    .build();

            PaymentHistoryResponse grpcResponse = paymentStub.getPaymentHistory(grpcRequest);

            return ResponseEntity.ok(
                    grpcResponse.getPaymentsList().stream().map(this::convertToDTO).toList()
            );

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while retrieving payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving payment history");
        }
    }

    /**
     * Mock: Get user information from User Service
     * In production, this would call the actual User Service via gRPC
     */
    private UserInfo getUserInfoFromUserService(String userId) {
        // Replace with actual gRPC call to User Service
        log.debug("Fetching user info for user ID: {}", userId);

        return UserInfo.newBuilder()
                .setUserId(userId)
                .setFirstName("John")
                .setLastName("Doe")
                .setStreet("Main Street")
                .setNumber(123)
                .setProvince("Ontario")
                .setCountry("Canada")
                .setPostalCode("M5H 2N2")
                .build();
    }

    /**
     * Mock: Get item details from Catalogue Service
     * In production, this would call the actual Catalogue Service via gRPC
     */
    private ItemDetails getItemDetailsFromCatalogueService(String itemId) {
        // TODO: Replace with actual gRPC call to Catalogue Service
        log.debug("Fetching item details for item ID: {}", itemId);

        return new ItemDetails(itemId, 100, 15, 5);
    }

    /**
     * Calculate shipping cost with expedited surcharge
     */
    private int calculateShippingCost(PaymentRequestDTO.ShippingTypeDTO shippingType,
                                         int baseShippingCost) {
//        if (shippingType == PaymentRequestDTO.ShippingTypeDTO.EXPEDITED) {
//            return baseShippingCost; // Surcharge will be added by Payment Service
//        }
        return baseShippingCost;
    }

    /**
     * Build gRPC PaymentRequest from REST request and aggregated data
     */
    private PaymentRequest buildGrpcPaymentRequest(
            PaymentRequestDTO request,
            UserInfo userInfo,
            ItemDetails itemDetails,
            int shippingCost) {

        CreditCardInfo creditCardInfo = CreditCardInfo.newBuilder()
                .setCardNumber(request.getCreditCard().getCardNumber())
                .setNameOnCard(request.getCreditCard().getNameOnCard())
                .setExpiryDate(request.getCreditCard().getExpiryDate())
                .setSecurityCode(request.getCreditCard().getSecurityCode())
                .build();

        ShippingInfo shippingInfo = ShippingInfo.newBuilder()
                .setShippingType(request.getShippingType() == PaymentRequestDTO.ShippingTypeDTO.EXPEDITED
                        ? ShippingType.EXPEDITED : ShippingType.REGULAR)
                .setShippingCost(shippingCost)
                .setEstimatedDays(itemDetails.getEstimatedShippingDays())
                .build();

        return PaymentRequest.newBuilder()
                .setUserInfo(userInfo)
                .setItemId(itemDetails.getItemId())
                .setItemCost(itemDetails.getItemCost())
                .setShippingInfo(shippingInfo)
                .setCreditCardInfo(creditCardInfo)
                .build();
    }

    /**
     * Convert gRPC PaymentResponse to REST DTO
     */
    private PaymentResponseDTO convertToDTO(PaymentResponse grpcResponse) {
        PaymentResponseDTO.PaymentResponseDTOBuilder builder = PaymentResponseDTO.builder()
                .success(grpcResponse.getSuccess())
                .paymentId(grpcResponse.getPaymentId())
                .message(grpcResponse.getMessage())
                .transactionDate(grpcResponse.getTransactionDate())
                .shippingMessage(grpcResponse.getShippingMessage());

        if (grpcResponse.hasReceiptInfo()) {
            ReceiptInfo receiptInfo = grpcResponse.getReceiptInfo();
            PaymentResponseDTO.ReceiptDTO receiptDTO = PaymentResponseDTO.ReceiptDTO.builder()
                    .receiptId(receiptInfo.getReceiptId())
                    .firstName(receiptInfo.getFirstName())
                    .lastName(receiptInfo.getLastName())
                    .address(receiptInfo.getFullAddress())
                    .itemCost(receiptInfo.getItemCost())
                    .shippingCost(receiptInfo.getShippingCost())
                    .hstAmount(receiptInfo.getHstAmount())
                    .totalPaid(receiptInfo.getTotalPaid())
                    .itemId(receiptInfo.getItemId())
                    .build();

            builder.receipt(receiptDTO);
        }

        return builder.build();
    }

    /**
     * Helper class to hold item details from Catalogue Service
     */
    private static class ItemDetails {
        private final String itemId;
        private final int itemCost;
        private final int baseShippingCost;
        private final int estimatedShippingDays;

        public ItemDetails(String itemId, int itemCost, int baseShippingCost, int estimatedShippingDays) {
            this.itemId = itemId;
            this.itemCost = itemCost;
            this.baseShippingCost = baseShippingCost;
            this.estimatedShippingDays = estimatedShippingDays;
        }

        public String getItemId() { return itemId; }
        public int getItemCost() { return itemCost; }
        public int getBaseShippingCost() { return baseShippingCost; }
        public int getEstimatedShippingDays() { return estimatedShippingDays; }
    }
}
