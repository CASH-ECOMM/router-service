package com.cash.controllers;

import com.cash.dtos.TotalCostDTO;
import com.ecommerce.payment.grpc.*;
import com.cash.dtos.PaymentRequestDTO;
import com.cash.dtos.PaymentResponseDTO;
import com.cash.mappers.PaymentServiceDtoMapper;
import com.cash.services.PaymentService;
import com.cash.services.CatalogueService;
import com.cash.grpc.catalogue.ItemResponse;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import com.cash.grpc.userservice.GetUserResponse;
import com.cash.grpc.userservice.Address;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Payment Router", description = "Payment routing endpoints for payment processing")
public class PaymentRouterController {

    private final PaymentService paymentClient;
    private final CatalogueService catalogueService;
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
            GetUserResponse user = getUserFromUserService(request.getUserId());

            // Get item details from Catalogue Service (mock for now)
            ItemDetails itemDetails = getItemDetailsFromCatalogueService(request.getItemId());

            // Calculate shipping cost based on type
            int shippingCost = calculateShippingCost(request.getShippingType(), itemDetails.getBaseShippingCost());

            PaymentRequest grpcReq = PaymentServiceDtoMapper.toProto(
                    request,
                    user,
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
     * Estimate total cost (item + shipping + HST)
     * Uses the same aggregation as /process but calls CalculateTotalCost RPC.
     */
    @PostMapping("/total-cost")
    @Operation(
            summary = "Estimate total cost",
            description = "Returns item cost, shipping, HST rate/amount, and total without charging the card"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Total cost calculated",
                    content = @Content(schema = @Schema(implementation = TotalCostDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> calculateTotalCost(@Valid @RequestBody PaymentRequestDTO request) {
        log.info("Calculating total cost for user: {} and item: {}", request.getUserId(), request.getItemId());

        try {
            // Same aggregation: get user & item (mocked here; replace with real gRPC calls)
            GetUserResponse user = getUserFromUserService(request.getUserId());
            ItemDetails itemDetails = getItemDetailsFromCatalogueService(request.getItemId());

            int shippingCost = calculateShippingCost(request.getShippingType(), itemDetails.getBaseShippingCost());

            // Build proto request (province defaulted to 'Ontario' inside mapper)
            PaymentRequest grpcReq = PaymentServiceDtoMapper.toProto(
                    request,
                    user,
                    itemDetails.getItemId(),
                    itemDetails.getItemCost(),
                    shippingCost,
                    itemDetails.getEstimatedShippingDays()
            );

            // Call RPC
            TotalCostResponse rpcResp = paymentClient.calculateTotalCost(grpcReq);

            // Map to REST DTO
            TotalCostDTO dto = PaymentServiceDtoMapper.fromProto(rpcResp);

            return ResponseEntity.ok(dto);

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while calculating total cost", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment service error: " + e.getStatus().getDescription()));
        } catch (Exception e) {
            log.error("Unexpected error while calculating total cost", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
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
    public ResponseEntity<PaymentResponseDTO> getReceipt(
            @Parameter(name = "paymentId", description = "Get Receipt for each payment", required = true)
            @PathVariable String paymentId) {
        log.info("Retrieving payment with ID: {}", paymentId);

        try {
            PaymentResponse grpcResp = paymentClient.getPaymentById(Integer.parseInt(paymentId));
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
            @PathVariable int userId,
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
    private GetUserResponse getUserFromUserService(int userId) {
        log.debug("Fetching user info for user ID: {}", userId);

        Address shipping = Address.newBuilder()
                .setStreetName("Main Street")
                .setStreetNumber("123")
                .setCity("Toronto")
                .setCountry("Canada")
                .setPostalCode("M5H 2N2")
                .build();

        return GetUserResponse.newBuilder()
                .setSuccess(true)
                .setUserId(userId)
                .setUsername("john.doe")
                .setFirstName("John")
                .setLastName("Doe")
                .setEmail("john.doe@example.com")
                .setShippingAddress(shipping)
                .setMessage("mock")
                .build();
    }

    /**
     * Mock: Get item details from Catalogue Service
     * In production, this would call the actual Catalogue Service via gRPC
     */
    private ItemDetails getItemDetailsFromCatalogueService(int itemId) {
        log.debug("Fetching item details for item ID: {}", itemId);

        ItemResponse ir = catalogueService.getItem(itemId);


        int itemCost = ir.getCurrentPrice() != 0 ? ir.getCurrentPrice()
                : (ir.getStartingPrice() != 0 ? ir.getStartingPrice() : 0);
        int shippingCost = ir.getShippingCost();
        int shippingDays = ir.getShippingTime() != 0 ? ir.getShippingTime() : 5;

        return new ItemDetails(ir.getId(), itemCost, shippingCost, shippingDays);
    }

    /**
     * Calculate shipping cost with expedited surcharge
     */
    private int calculateShippingCost(PaymentRequestDTO.ShippingTypeDTO shippingType,
                                      int baseShippingCost) {
        return baseShippingCost;
    }


    /**
     * Helper class to hold item details from Catalogue Service
     */
    private static class ItemDetails {
        private final int itemId;
        private final int itemCost;
        private final int baseShippingCost;
        private final int estimatedShippingDays;

        public ItemDetails(int itemId, int itemCost, int baseShippingCost, int estimatedShippingDays) {
            this.itemId = itemId;
            this.itemCost = itemCost;
            this.baseShippingCost = baseShippingCost;
            this.estimatedShippingDays = estimatedShippingDays;
        }

        public int getItemId() { return itemId; }
        public int getItemCost() { return itemCost; }
        public int getBaseShippingCost() { return baseShippingCost; }
        public int getEstimatedShippingDays() { return estimatedShippingDays; }
    }
}

