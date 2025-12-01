package com.cash.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import com.cash.dtos.TotalCostDTO;
import com.cash.exceptions.ConflictException;
import com.cash.exceptions.UnauthorizedException;
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
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.cash.grpc.userservice.GetUserResponse;

import java.util.List;
import java.util.stream.Collectors;

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
    @Operation(summary = "Process payment", description = "Process payment by aggregating user info, catalogue data, and credit card information")
    @ApiResponse(responseCode = "200", description = "Payment processed successfully", content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class)))
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO request,
            jakarta.servlet.http.HttpServletRequest httpReq // read auth attrs
    ) {
        Integer authedUserId = com.cash.config.AuthenticatedUser.getUserId(httpReq);
        if (authedUserId == null || authedUserId <= 0) {
            throw new UnauthorizedException("No authenticated user in request");
        }
        log.info("Received payment request for user: {} (from interceptor) and item: {}",
                authedUserId, request.getItemId());
        if (request.getItemId() <= 0) {
            throw new IllegalArgumentException("Invalid itemId");
        }

        try {
            // Get user information from User Service
            GetUserResponse user = userService.getUser(authedUserId);

            // Get item details from Catalogue Service
            ItemDetails itemDetails = getItemDetailsFromCatalogueService(request.getItemId());

            // Verfiy auction winner before payment
            var winnerResponse = auctionService.getAuctionWinner(request.getItemId());
            if (!winnerResponse.getFound()) {
                // auction not ended or no winner yet
                log.warn("Auction winner not found for catalogue {}", request.getItemId());
                throw new ConflictException("Auction has not ended or no winner is determined yet.");
            }
            if (winnerResponse.getWinnerUserId() != authedUserId) {
                // caller is not the winner
                throw new UnauthorizedException("You are not the winning bidder for this item.");
            }
            int finalAuctionPrice = winnerResponse.getFinalPrice();
            // Calculate shipping cost based on type
            int shippingCost = calculateShippingCost(request.getShippingType(), itemDetails.getBaseShippingCost());

            PaymentRequest grpcReq = PaymentServiceDtoMapper.toProto(
                    request,
                    user,
                    authedUserId,
                    itemDetails.getItemId(),
                    finalAuctionPrice, // int (whole dollars)
                    shippingCost, // int (whole dollars)
                    itemDetails.estimatedShippingDays);

            // Build gRPC payment request
            PaymentResponse grpcResp = paymentClient.processPayment(grpcReq);

            // Call Payment Service via gRPC
            PaymentResponseDTO dto = PaymentServiceDtoMapper.fromProto(grpcResp);

            log.info("Payment processed successfully with ID: {}", dto.getPaymentId());

            if (grpcResp.getSuccess()) {
                try {
                    log.info("Item {} deactivated after successful payment. Message: {}",
                            request.getItemId(),
                            catalogueService.deactivateItem(request.getItemId()).getMessage());
                } catch (Exception e) {
                    log.error("Failed to deactivate item {} after payment", request.getItemId(), e);
                }
            }

            // Add HATEOAS links
            dto.add(linkTo(methodOn(PaymentRouterController.class)
                    .processPayment(request, httpReq)).withSelfRel());
            dto.add(linkTo(methodOn(PaymentRouterController.class)
                    .getReceipt(String.valueOf(dto.getPaymentId()))).withRel("receipt"));
            dto.add(linkTo(methodOn(PaymentRouterController.class)
                    .getMyPaymentHistory(httpReq)).withRel("payment-history"));
            dto.add(linkTo(methodOn(CatalogueController.class)
                    .getItem(request.getItemId())).withRel("catalogue-item"));

            return ResponseEntity.ok(dto);

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while processing payment", e);
            throw e; // Let GlobalExceptionHandler handle gRPC exceptions

        } catch (Exception e) {
            log.error("Error processing payment", e);
            throw e; // Let GlobalExceptionHandler handle generic exceptions
        }
    }

    /**
     * Estimate total cost (item + shipping + HST)
     * Uses the same aggregation as /process but calls CalculateTotalCost RPC.
     */
    @PostMapping("/total-cost")
    @Operation(summary = "Total cost", description = "Returns item cost, shipping, HST rate/amount, and total without charging the card")
    @ApiResponse(responseCode = "200", description = "Total cost calculated", content = @Content(schema = @Schema(implementation = TotalCostDTO.class)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.cash.dtos.TotalCostRequestDTO.class), examples = {
            @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Regular shipping", summary = "REGULAR", description = "Choose REGULAR (no surcharge).", value = "{\n  \"item_id\": 1001,\n  \"shipping_type\": \"REGULAR\"\n}"),
            @io.swagger.v3.oas.annotations.media.ExampleObject(name = "Expedited shipping", summary = "EXPEDITED", description = "Choose EXPEDITED (adds expedited surcharge).", value = "{\n  \"item_id\": 1001,\n  \"shipping_type\": \"EXPEDITED\"\n}")
    }))
    public ResponseEntity<?> calculateTotalCost(@Valid @RequestBody com.cash.dtos.TotalCostRequestDTO request) {
        log.info("Calculating total cost for item: {}", request.getItemId());

        try {
            // Require auction to have ended so we can use the final auction price
            var winnerResponse = auctionService.getAuctionWinner(request.getItemId());
            if (!winnerResponse.getFound()) {
                throw new ConflictException(
                        "Auction has not ended yet; total cost will be available after a winner is determined.");
            }
            // User winner

            ItemDetails itemDetails = getItemDetailsFromCatalogueService(request.getItemId());
            // int baseCataloguePrice = itemDetails.getItemCost();
            int finalAuctionPrice = winnerResponse.getFinalPrice();
            // base from catalogue; surcharge applied inside payment-service if EXPEDITED
            var shipType = request.getShippingType() == null
                    ? PaymentRequestDTO.ShippingTypeDTO.REGULAR
                    : request.getShippingType();

            int shippingCost = calculateShippingCost(shipType, itemDetails.getBaseShippingCost());

            // Build proto *without* credit-card (quote only)
            PaymentRequest grpcReq = PaymentServiceDtoMapper.toProtoQuote(
                    itemDetails.getItemId(),
                    finalAuctionPrice,
                    shippingCost,
                    itemDetails.getEstimatedShippingDays(),
                    shipType);

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

            // Add HATEOAS links
            dto.add(linkTo(methodOn(PaymentRouterController.class)
                    .calculateTotalCost(request)).withSelfRel());
            dto.add(linkTo(methodOn(CatalogueController.class)
                    .getItem(request.getItemId())).withRel("catalogue-item"));
            dto.add(linkTo(methodOn(AuctionController.class)
                    .getAuctionWinner(request.getItemId())).withRel("auction-winner"));

            return ResponseEntity.ok(dto);

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while calculating total cost", e);
            throw e; // Let GlobalExceptionHandler handle gRPC exceptions
        } catch (Exception e) {
            log.error("Unexpected error while calculating total cost", e);
            throw e; // Let GlobalExceptionHandler handle generic exceptions
        }
    }

    /**
     * Use Case 6: Get Payment Receipt
     * Retrieve payment details and receipt information
     */
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get Receipt by ID", description = "Retrieve payment details and receipt information")
    @ApiResponse(responseCode = "200", description = "Payment found", content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class)))
    public ResponseEntity<PaymentResponseDTO> getReceipt(
            @Parameter(name = "paymentId", description = "Get Receipt for each payment", required = true) @PathVariable String paymentId) {
        log.info("Retrieving payment with ID: {}", paymentId);
        final int pid;
        try {
            pid = Integer.parseInt(paymentId);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("paymentId must be an integer");
        }

        try {
            PaymentResponse grpcResp = paymentClient.getPaymentById(pid);
            if (!grpcResp.getSuccess()) {
                throw new com.cash.exceptions.ResourceNotFoundException("Payment not found");
            }
            PaymentResponseDTO dto = PaymentServiceDtoMapper.fromProto(grpcResp);

            // Add HATEOAS links
            dto.add(linkTo(methodOn(PaymentRouterController.class)
                    .getReceipt(paymentId)).withSelfRel());
            dto.add(linkTo(methodOn(PaymentRouterController.class)
                    .getMyPaymentHistory(null)).withRel("payment-history"));

            return ResponseEntity.ok(dto);

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while retrieving payment", e);
            throw e; // Let GlobalExceptionHandler handle gRPC exceptions
        }
    }

    /**
     * Get Payment History
     */
    private static final int DEFAULT_HISTORY_SIZE = 10;

    @GetMapping("/history")
    @Operation(summary = "My payment history (authenticated)", description = "Returns payment history for the authenticated user. No input parameters.")
    @ApiResponse(responseCode = "200", description = "History returned")
    public ResponseEntity<CollectionModel<PaymentResponseDTO>> getMyPaymentHistory(
            jakarta.servlet.http.HttpServletRequest httpReq // CHANGE: read auth attrs
    ) {
        Integer authedUserId = com.cash.config.AuthenticatedUser.getUserId(httpReq);
        if (authedUserId == null || authedUserId <= 0) {
            throw new UnauthorizedException("No authenticated user in request");
        }

        log.info("Retrieving payment history for authenticated user: {}", authedUserId);

        try {
            // internally pick a default window
            PaymentHistoryResponse resp = paymentClient.getHistory(authedUserId, 0, DEFAULT_HISTORY_SIZE);

            List<PaymentResponseDTO> paymentList = resp.getPaymentsList().stream()
                    .map(PaymentServiceDtoMapper::fromProto)
                    .map(this::addLinksToPayment)
                    .collect(Collectors.toList());

            // Create CollectionModel with links
            CollectionModel<PaymentResponseDTO> collectionModel = CollectionModel.of(paymentList);
            collectionModel.add(linkTo(methodOn(PaymentRouterController.class)
                    .getMyPaymentHistory(httpReq)).withSelfRel());

            return ResponseEntity.ok(collectionModel);
        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC error while retrieving payment history", e);
            throw e; // Let GlobalExceptionHandler handle gRPC exceptions
        } catch (Exception e) {
            log.error("Unexpected error while retrieving payment history", e);
            throw e; // Let GlobalExceptionHandler handle generic exceptions
        }
    }

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
     * Helper method to add HATEOAS links to a payment response
     */
    private PaymentResponseDTO addLinksToPayment(PaymentResponseDTO payment) {
        if (payment.getPaymentId() != null) {
            payment.add(linkTo(methodOn(PaymentRouterController.class)
                    .getReceipt(String.valueOf(payment.getPaymentId()))).withRel("receipt"));
        }
        payment.add(linkTo(methodOn(PaymentRouterController.class)
                .getMyPaymentHistory(null)).withRel("payment-history"));
        return payment;
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

        public int getItemId() {
            return itemId;
        }

        public int getItemCost() {
            return itemCost;
        }

        public int getBaseShippingCost() {
            return baseShippingCost;
        }

        public int getEstimatedShippingDays() {
            return estimatedShippingDays;
        }
    }
}
