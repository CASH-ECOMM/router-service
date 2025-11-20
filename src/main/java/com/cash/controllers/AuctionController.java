package com.cash.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import com.cash.config.AuthenticatedUser;
import com.cash.config.BiddingSessionManager;
import com.cash.dtos.*;
import com.cash.exceptions.ResourceNotFoundException;
import com.cash.exceptions.UnauthorizedException;
import com.cash.grpc.auctionservice.*;
import com.cash.grpc.catalogue.ItemList;
import com.cash.grpc.catalogue.ItemResponse;
import com.cash.mappers.AuctionServiceDtoMapper;
import com.cash.mappers.CatalogueServiceDtoMapper;
import com.cash.services.AuctionService;
import com.cash.services.CatalogueService;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.List;

import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@Tag(name = "Auctions", description = "Auction management and bidding operations")
public class AuctionController {
    private final AuctionService auctionService;
    private final BiddingSessionManager biddingSessionManager;
    private final CatalogueService catalogueService;

    @Autowired
    public AuctionController(
            AuctionService auctionService,
            BiddingSessionManager biddingSessionManager,
            CatalogueService catalogueService) {
        this.auctionService = auctionService;
        this.biddingSessionManager = biddingSessionManager;
        this.catalogueService = catalogueService;
    }

    @ApiResponse(responseCode = "200", description = "Auction started successfully", content = @Content(schema = @Schema(implementation = StartAuctionResponseDto.class)))
    @PostMapping("/{catalogueId}/start")
    public ResponseEntity<StartAuctionResponseDto> startAuction(
            @Parameter(description = "Catalogue item ID", required = true) @PathVariable int catalogueId,
            HttpServletRequest request) {
        Integer authUser = AuthenticatedUser.getUserId(request);
        if (authUser == null) {
            throw new UnauthorizedException("You need to be logged in to start an auction.");
        }

        // Gets the item from the catalogue service
        ItemResponse item = catalogueService.getItem(catalogueId);

        String endTime = item.getEndTime();

        // assume it's UTC
        Instant instant = LocalDateTime.parse(endTime).toInstant(ZoneOffset.UTC);

        Timestamp protoTimestamp = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        StartAuctionResponse response = auctionService.startAuction(
                authUser,
                catalogueId,
                item.getStartingPrice(),
                protoTimestamp // Converted end time from string to protobuf Timestamp
        );

        if (!response.getSuccess()) {
            throw new IllegalArgumentException(response.getMessage());
        }

        StartAuctionResponseDto dto = StartAuctionResponseDto.builder()
                .success(true)
                .message(response.getMessage())
                .catalogueId(catalogueId)
                .build();

        // Add HATEOAS links
        dto.add(
                linkTo(methodOn(AuctionController.class).startAuction(catalogueId, request)).withSelfRel());
        dto.add(
                linkTo(methodOn(AuctionController.class).getAuctionStatus(catalogueId))
                        .withRel("auction-status"));
        dto.add(
                linkTo(methodOn(AuctionController.class).placeBid(catalogueId, null, request, null))
                        .withRel("place-bid"));
        dto.add(
                linkTo(methodOn(CatalogueController.class).getItem(catalogueId)).withRel("catalogue-item"));

        return ResponseEntity.ok(dto);
    }

    @ApiResponse(responseCode = "200", description = "Bid placed successfully", content = @Content(schema = @Schema(implementation = PlaceBidResponseDto.class)))
    @PostMapping("/{catalogueId}/bid")
    public ResponseEntity<PlaceBidResponseDto> placeBid(
            @Parameter(description = "Catalogue item ID", required = true) @PathVariable int catalogueId,
            @Parameter(description = "Bid details", required = true) @RequestBody PlaceBidRequestDto dto,
            HttpServletRequest request,
            HttpSession session) {
        Integer authUser = AuthenticatedUser.getUserId(request);
        String authUsername = AuthenticatedUser.getUsername(request);
        Integer currentItemBid = biddingSessionManager.getItem(session);

        if (authUser == null) {
            throw new UnauthorizedException("You need to be logged in to place a bid.");
        }

        PlaceBidRequest bidRequest = AuctionServiceDtoMapper.toProto(dto);

        // Gets the item from the catalogue service
        ItemResponse item = catalogueService.getItem(catalogueId);

        if (currentItemBid != null && !currentItemBid.equals(catalogueId)) {
            throw new IllegalArgumentException(
                    "You can only bid on one auction item at a time. Please finish bidding on your current item before placing a bid on another.");
        }

        // Checks if the authenticated user is the seller of the item
        if (item.getSellerId() == authUser) {
            throw new IllegalArgumentException("You cannot place a bid on your own auction item.");
        }

        PlaceBidResponse response = auctionService.placeBid(authUser, authUsername, catalogueId,
                bidRequest.getAmount());

        if (!response.getSuccess()) {
            throw new IllegalArgumentException(response.getMessage());
        }

        biddingSessionManager.setItem(session, item.getId());

        PlaceBidResponseDto responseDto = PlaceBidResponseDto.builder()
                .success(true)
                .message(response.getMessage())
                .catalogueId(catalogueId)
                .build();

        // Add HATEOAS links
        responseDto.add(
                linkTo(methodOn(AuctionController.class).placeBid(catalogueId, dto, request, session))
                        .withSelfRel());
        responseDto.add(
                linkTo(methodOn(AuctionController.class).getAuctionStatus(catalogueId))
                        .withRel("auction-status"));
        responseDto.add(
                linkTo(methodOn(AuctionController.class).getAuctionEnd(catalogueId))
                        .withRel("auction-end"));
        responseDto.add(
                linkTo(methodOn(CatalogueController.class).getItem(catalogueId)).withRel("catalogue-item"));

        return ResponseEntity.ok(responseDto);
    }

    @ApiResponse(responseCode = "200", description = "End time retrieved successfully", content = @Content(schema = @Schema(implementation = EndTimeResponseDto.class)))
    @GetMapping("/{catalogueId}/end")
    public ResponseEntity<EndTimeResponseDto> getAuctionEnd(
            @Parameter(description = "Catalogue item ID", required = true) @PathVariable int catalogueId) {
        GetAuctionEndResponse response = auctionService.getAuctionEnd(catalogueId);

        if (!response.getFound()) {
            throw new ResourceNotFoundException(response.getMessage());
        }

        EndTimeResponseDto dto = AuctionServiceDtoMapper.fromProto(response);
        dto.setCatalogueId(catalogueId);

        // Add HATEOAS links
        dto.add(linkTo(methodOn(AuctionController.class).getAuctionEnd(catalogueId)).withSelfRel());
        dto.add(
                linkTo(methodOn(AuctionController.class).getAuctionStatus(catalogueId))
                        .withRel("auction-status"));
        dto.add(
                linkTo(methodOn(AuctionController.class).getAuctionWinner(catalogueId))
                        .withRel("auction-winner"));
        dto.add(
                linkTo(methodOn(CatalogueController.class).getItem(catalogueId)).withRel("catalogue-item"));

        return ResponseEntity.ok(dto);
    }

    @ApiResponse(responseCode = "200", description = "Status retrieved successfully", content = @Content(schema = @Schema(implementation = AuctionStatusResponseDto.class)))
    @GetMapping("/{catalogueId}/status")
    public ResponseEntity<AuctionStatusResponseDto> getAuctionStatus(
            @Parameter(description = "Catalogue item ID", required = true) @PathVariable int catalogueId) {
        GetAuctionStatusResponse response = auctionService.getAuctionStatus(catalogueId);

        if (!response.getSuccess()) {
            throw new ResourceNotFoundException(response.getMessage());
        }

        AuctionStatusResponseDto dto = AuctionStatusResponseDto.builder()
                .highestBidder(response.getHighestBidder())
                .currentHighestBid(response.getCurrentAmount())
                .remainingTime(response.getRemainingTime())
                .auctionStatus(response.getMessage())
                .catalogueId(catalogueId)
                .build();

        // Add HATEOAS links
        dto.add(linkTo(methodOn(AuctionController.class).getAuctionStatus(catalogueId)).withSelfRel());
        dto.add(
                linkTo(methodOn(CatalogueController.class).getItem(catalogueId)).withRel("catalogue-item"));

        // Conditional links based on auction state
        if (response.getRemainingTime() > 0) {
            // Auction is still active - allow bidding
            dto.add(
                    linkTo(methodOn(AuctionController.class).placeBid(catalogueId, null, null, null))
                            .withRel("place-bid"));
            dto.add(
                    linkTo(methodOn(AuctionController.class).getAuctionEnd(catalogueId))
                            .withRel("auction-end"));
        } else {
            // Auction ended - show winner
            dto.add(
                    linkTo(methodOn(AuctionController.class).getAuctionWinner(catalogueId))
                            .withRel("auction-winner"));
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * Gets the winner of an auction
     *
     * @param catalogueId The ID of the catalogue item
     * @return ResponseEntity with auction winner details
     */
    @Operation(summary = "Get auction winner", description = "Retrieves the winner of a completed auction")
    @ApiResponse(responseCode = "200", description = "Winner information retrieved successfully", content = @Content(schema = @Schema(implementation = AuctionWinnerResponseDto.class)))
    @GetMapping("/{catalogueId}/winner")
    public ResponseEntity<AuctionWinnerResponseDto> getAuctionWinner(
            @Parameter(description = "Catalogue item ID", required = true) @PathVariable int catalogueId) {
        GetAuctionWinnerResponse response = auctionService.getAuctionWinner(catalogueId);

        if (!response.getFound()) {
            throw new ResourceNotFoundException(response.getMessage());
        }

        AuctionWinnerResponseDto dto = AuctionWinnerResponseDto.builder()
                .found(response.getFound())
                .winningUserId(response.getWinnerUserId())
                .finalPrice(response.getFinalPrice())
                .message(response.getMessage())
                .catalogueId(catalogueId)
                .build();

        // Add HATEOAS links
        dto.add(linkTo(methodOn(AuctionController.class).getAuctionWinner(catalogueId)).withSelfRel());
        dto.add(
                linkTo(methodOn(AuctionController.class).getAuctionStatus(catalogueId))
                        .withRel("auction-status"));
        dto.add(
                linkTo(methodOn(CatalogueController.class).getItem(catalogueId)).withRel("catalogue-item"));
        dto.add(
                linkTo(methodOn(AuctionController.class).getAuctionEnd(catalogueId))
                        .withRel("auction-end"));

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{userId}/wins")
    public ResponseEntity<List<AuctionWinsResponseDto>> getUserWins(@Parameter(description = "User Id", required = true) @PathVariable int userId,
                                                                    HttpServletRequest request) {

        Integer authUser = AuthenticatedUser.getUserId(request);

        if (authUser == null) {
            throw new UnauthorizedException("You need to be logged in to view your auction wins.");
        }

        if (authUser != userId) {
            throw new UnauthorizedException("You can only view your own auction wins.");
        }

        ItemList response = catalogueService.getAllItems();
        List<CatalogueItemResponseDto> items = CatalogueServiceDtoMapper.fromProtoList(response.getItemsList());
        List<AuctionWinsResponseDto> wins = new java.util.ArrayList<>();

        for (CatalogueItemResponseDto item : items) {
            GetAuctionWinnerResponse auctionWinnerResponse = auctionService.getAuctionWinner(item.getId());
            if (auctionWinnerResponse.getFound() && auctionWinnerResponse.getWinnerUserId() == userId) {
                AuctionWinsResponseDto winDto = AuctionWinsResponseDto.builder()
                        .catalogueId(item.getId())
                        .finalPrice(auctionWinnerResponse.getFinalPrice())
                        .build();
                wins.add(winDto);
            }
        }

        return ResponseEntity.ok(wins);
    }
}
