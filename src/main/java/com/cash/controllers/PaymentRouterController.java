package com.cash.controllers;

import com.ecommerce.payment.grpc.*;
import com.cash.dtos.PaymentRequestDTO;
import com.cash.dtos.PaymentResponseDTO;
import com.cash.mappers.PaymentServiceDtoMapper;
import com.cash.services.PaymentService;
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

    private final PaymentService paymentClient;
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

            PaymentRequest grpcReq = PaymentServiceDtoMapper.toProto(
                    request,
                    userInfo,
                    itemDetails.getItemId(),
                    itemDetails.getItemCost(),               // int (whole dollars)
                    shippingCost,                     // int (whole dollars)
                    itemDetails.getEstimatedShippingDays()
            );

            // Build gRPC payment request
            PaymentResponse grpcResp = paymentClient.processPayment(grpcReq);

            // Call Payment Service via gRPC
            PaymentResponseDTO dto = PaymentServiceDtoMapper.fromProto(grpcResp);

            log.info("Payment processed successfully with ID: {}", dto.getPaymentId());

            return ResponseEntity.ok(dto);

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
            PaymentResponse grpcResp = paymentClient.getPaymentById(paymentId);
            if (!grpcResp.getSuccess()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(PaymentServiceDtoMapper.fromProto(grpcResp)); // CHANGE: mapper
            }

            return ResponseEntity.ok(PaymentServiceDtoMapper.fromProto(grpcResp));

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
            PaymentHistoryResponse resp = paymentClient.getHistory(userId, page, size);


            return ResponseEntity.ok(
                    resp.getPaymentsList().stream()
                            .map(PaymentServiceDtoMapper::fromProto)
                            .toList()
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

