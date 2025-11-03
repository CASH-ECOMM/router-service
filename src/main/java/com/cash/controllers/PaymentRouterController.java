package com.cash.controllers;

import com.cash.dtos.TotalCostDTO;
import com.cash.grpc.userservice.ValidateTokenResponse;
import com.ecommerce.payment.grpc.*;
import com.cash.dtos.PaymentRequestDTO;
import com.cash.dtos.PaymentResponseDTO;
import com.cash.mappers.PaymentServiceDtoMapper;
import com.cash.services.PaymentService;
import com.cash.services.CatalogueService;
import com.cash.services.UserService;
import com.cash.services.AuctionService;
import com.cash.grpc.catalogue.ItemResponse;

import io.grpc.StatusRuntimeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.cash.grpc.userservice.GetUserResponse;

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
    private final UserService userService;
    private final AuctionService auctionService;
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
            @Valid @RequestBody PaymentRequestDTO request,
            jakarta.servlet.http.HttpServletRequest httpReq // read auth attrs
    ) {
        Integer authedUserId = com.cash.config.AuthenticatedUser.getUserId(httpReq);
        if (authedUserId == null || authedUserId <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(PaymentResponseDTO.builder()
                            .success(false)
                            .message("No authenticated user in request")
                            .build());
        }
        log.info("Received payment request for user: {} (from interceptor) and item: {}",
                authedUserId, request.getItemId());
        if (request.getItemId() <= 0) {
            return ResponseEntity.badRequest()
                    .body(PaymentResponseDTO.builder().success(false).message("Invalid itemId").build());
        }

        try {
            // Get user information from User Service
            GetUserResponse user = userService.getUser(authedUserId);
//            if (!user.getSuccess()) {
//                log.warn("User lookup failed for user {}", request.getUserId());
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(PaymentResponseDTO.builder()
//                                .success(false)
//                                .message("User lookup failed: " + user.getMessage())
//                                .build());
//            }
//            if (user.getShippingAddress() == null) {
//                // Avoid letting mapper throw and turning this into a 500
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(PaymentResponseDTO.builder()
//                                .success(false)
//                                .message("User has no shipping address on file")
//                                .build());
//            }

            // Get item details from Catalogue Service
            ItemDetails itemDetails = getItemDetailsFromCatalogueService(request.getItemId());

            //Verfiy auction winner before payment
            var winnerResponse = auctionService.getAuctionWinner(request.getItemId());
            if (!winnerResponse.getFound()) {
                // auction not ended or no winner yet
                log.warn("Auction winner not found for catalogue {}", request.getItemId());
                return ResponseEntity.status(HttpStatus.CONFLICT) // 409
                        .body(PaymentResponseDTO.builder()
                                .success(false)
                                .message("Auction has not ended or no winner is determined yet.")
                                .build());
            }
            if (winnerResponse.getWinnerUserId() != authedUserId) {
                // caller is not the winner
                return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403
                        .body(PaymentResponseDTO.builder()
                                .success(false)
                                .message("You are not the winning bidder for this item.")
                                .build());
            }
            int finalAuctionPrice = winnerResponse.getFinalPrice();
            // Calculate shipping cost based on type
            int shippingCost = calculateShippingCost(request.getShippingType(), itemDetails.getBaseShippingCost());

            PaymentRequest grpcReq = PaymentServiceDtoMapper.toProto(
                    request,
                    user,
                    authedUserId,
                    itemDetails.getItemId(),
                    finalAuctionPrice,               // int (whole dollars)
                    shippingCost,                            // int (whole dollars)
                    itemDetails.estimatedShippingDays
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
            summary = "Total cost",
            description = "Returns item cost, shipping, HST rate/amount, and total without charging the card"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Total cost calculated",
                    content = @Content(schema = @Schema(implementation = TotalCostDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Auction has not ended"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.cash.dtos.TotalCostRequestDTO.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Regular shipping",
                                    summary = "REGULAR",
                                    description = "Choose REGULAR (no surcharge).",
                                    value = "{\n  \"item_id\": 1001,\n  \"shipping_type\": \"REGULAR\"\n}"
                            ),
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Expedited shipping",
                                    summary = "EXPEDITED",
                                    description = "Choose EXPEDITED (adds expedited surcharge).",
                                    value = "{\n  \"item_id\": 1001,\n  \"shipping_type\": \"EXPEDITED\"\n}"
                            )
                    }
            )
    )
    public ResponseEntity<?> calculateTotalCost(@Valid @RequestBody com.cash.dtos.TotalCostRequestDTO request){
        log.info("Calculating total cost for item: {}", request.getItemId());


        try {
            // Require auction to have ended so we can use the final auction price
            var winnerResponse = auctionService.getAuctionWinner(request.getItemId());
            if (!winnerResponse.getFound()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error",
                                "Auction has not ended yet; total cost will be available after a winner is determined."));
            }
            //User winner

            ItemDetails itemDetails = getItemDetailsFromCatalogueService(request.getItemId());
//            int baseCataloguePrice = itemDetails.getItemCost();
            int finalAuctionPrice = winnerResponse.getFinalPrice();
//            base from catalogue; surcharge applied inside payment-service if EXPEDITED
            var shipType = request.getShippingType() == null
                    ? PaymentRequestDTO.ShippingTypeDTO.REGULAR
                    : request.getShippingType();

            int shippingCost = calculateShippingCost(shipType, itemDetails.getBaseShippingCost());

            // Method 2: decide auction-based price
//            int priceToUse;
//            String priceSource;
//            try {
//                var winnerResp = auctionService.getAuctionWinner(request.getItemId());          // CHANGED
//                if (winnerResp.getFound()) {
//                    priceToUse = Math.max(0, winnerResp.getFinalPrice());                      // CHANGED: final price
//                    priceSource = "final auction price";
//                } else {
//                    var statusResp = auctionService.getAuctionStatus(request.getItemId());     // CHANGED: fallback to current bid
//                    priceToUse = Math.max(0, statusResp.getCurrentAmount());                   // CHANGED: current highest bid
//                    priceSource = "current highest bid";
//
//                    // OPTIONAL: if there are zero bids, you *may* prefer starting price (auction param)
//                    // int starting = Math.max(0, itemDetails.getItemCost());
//                    // if (priceToUse == 0 && starting > 0) {
//                    //     priceToUse = starting;
//                    //     priceSource = "auction starting price";
//                    // }
//                }
//            } catch (StatusRuntimeException ex) {
//                log.error("Auction service unavailable for item {}: {}", request.getItemId(), ex.getStatus());
//                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                        .body(Map.of("error", "Auction service unavailable to compute auction-based price"));
//            }

            // Build proto *without* credit-card (quote only)
            PaymentRequest grpcReq = PaymentServiceDtoMapper.toProtoQuote(
                    itemDetails.getItemId(),
                    finalAuctionPrice,
                    shippingCost,
                    itemDetails.getEstimatedShippingDays(),
                    shipType
            );

            // Call RPC
            TotalCostResponse rpc = paymentClient.calculateTotalCost(grpcReq);
            double derivedShipping = round2(rpc.getTotalCost() - rpc.getHstAmount() - rpc.getItemCost());
            int shippingCostEffective = (int) Math.round(derivedShipping);
            // Map to REST DTO
            TotalCostDTO dto = TotalCostDTO.builder()
                    .itemCost(rpc.getItemCost())
                    .shippingCost(shippingCostEffective)
                    .hstRate(rpc.getHstRate())
                    .hstAmount(rpc.getHstAmount())
                    .totalCost(rpc.getTotalCost())
                    .message(rpc.getMessage())
                    .build();

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
            summary = "Get Receipt by ID",
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
        final int pid;
        try {
            pid = Integer.parseInt(paymentId);
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body(
                    PaymentResponseDTO.builder()
                            .success(false)
                            .message("paymentId must be an integer")
                            .build());
        }

        try {
            PaymentResponse grpcResp = paymentClient.getPaymentById(pid);
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
    private static final int DEFAULT_HISTORY_SIZE = 10;
    @GetMapping("/history")
    @Operation(
            summary = "My payment history (authenticated)",
            description = "Returns payment history for the authenticated user. No input parameters."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History returned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getMyPaymentHistory(
            jakarta.servlet.http.HttpServletRequest httpReq // CHANGE: read auth attrs
    ){
        Integer authedUserId = com.cash.config.AuthenticatedUser.getUserId(httpReq);
        if (authedUserId == null || authedUserId <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No authenticated user in request"));
        }

        log.info("Retrieving payment history for authenticated user: {}", authedUserId);

        try {
            // internally pick a default window
            PaymentHistoryResponse resp = paymentClient.getHistory(authedUserId, 0, DEFAULT_HISTORY_SIZE);

            return ResponseEntity.ok(
                    resp.getPaymentsList().stream()
                            .map(PaymentServiceDtoMapper::fromProto)
                            .toList()
            );
        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC error while retrieving payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving payment history: " + e.getStatus().getDescription()));
        } catch (Exception e) {
            log.error("Unexpected error while retrieving payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mock: Get user information from User Service
     * In production, this would call the actual User Service via gRPC
     */
//    private GetUserResponse getUserFromUserService(int userId) {
//        log.debug("Fetching user info for user ID: {}", userId);
//
//        Address shipping = Address.newBuilder()
//                .setStreetName("Main Street")
//                .setStreetNumber("123")
//                .setCity("Toronto")
//                .setCountry("Canada")
//                .setPostalCode("M5H 2N2")
//                .build();
//
//        return GetUserResponse.newBuilder()
//                .setSuccess(true)
//                .setUserId(userId)
//                .setUsername("john.doe")
//                .setFirstName("John")
//                .setLastName("Doe")
//                .setEmail("john.doe@example.com")
//                .setShippingAddress(shipping)
//                .setMessage("mock")
//                .build();
//    }

    /**
     * Get item details from Catalogue Service
     * In production, this would call the actual Catalogue Service via gRPC
     */
    private ItemDetails getItemDetailsFromCatalogueService(int itemId) {
        log.debug("Fetching item details for item ID: {}", itemId);

        ItemResponse ir = catalogueService.getItem(itemId);
        if (ir.getId() == 0) {
            throw new IllegalArgumentException("Catalogue item " + itemId + " not found");
        }


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
    private static double round2(double v) {
        return new java.math.BigDecimal(v)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
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

