package com.cash.controllers;

import com.cash.config.AuthenticatedUser;
import com.cash.config.BiddingSessionManager;
import com.cash.dtos.*;
import com.cash.grpc.catalogue.ItemResponse;
import com.cash.services.AuctionService;
import com.cash.mappers.AuctionServiceDtoMapper;
import com.cash.grpc.auctionservice.*;
import com.cash.services.CatalogueService;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import com.google.protobuf.Timestamp;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {
    private final AuctionService auctionService;
    private final BiddingSessionManager biddingSessionManager;
    private final CatalogueService catalogueService;

    @Autowired
    public AuctionController(AuctionService auctionService, BiddingSessionManager biddingSessionManager, CatalogueService catalogueService) {
        this.auctionService = auctionService;
        this.biddingSessionManager = biddingSessionManager;
        this.catalogueService = catalogueService;
    }

    @PostMapping("/{catalogueId}/startauction")
    public ResponseEntity<?> startAuction(@PathVariable int catalogueId, HttpServletRequest request) {
        try {
            Integer authUser = AuthenticatedUser.getUserId(request);
            if (authUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "You need to be logged in to start an auction."));
            }

            ItemResponse item;

            try {
                // Gets the item from the catalogue service
                item = catalogueService.getItem(catalogueId);
            } catch (StatusRuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Item not found in catalogue."));
            }

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
                    protoTimestamp    // Converted end time from string to protobuf Timestamp
            );

            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of(
                        //"auctionId", response.getAuctionId(),
                        "message", response.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{catalogueId}/placebid")
    public ResponseEntity<?> placeBid(@PathVariable int catalogueId, @RequestBody PlaceBidRequestDto dto, HttpServletRequest request, HttpSession session) {
        try {
            Integer authUser = AuthenticatedUser.getUserId(request);
            String authUsername = AuthenticatedUser.getUsername(request);
            Integer currentItemBid = biddingSessionManager.getItem(session);

            if (authUser == null) {  // Need to implement a way for a user to not bid on their own auction
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You need to be logged in to place a bid."));
            }
            PlaceBidRequest bidRequest = AuctionServiceDtoMapper.toProto(dto);

            if (currentItemBid != null && !currentItemBid.equals(catalogueId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only bid on one auction item at a time. Please finish bidding on your current item before placing a bid on another."));
            }

            ItemResponse item;

            try {
                // Gets the item from the catalogue service
                item = catalogueService.getItem(catalogueId);
            } catch (StatusRuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Item not found in catalogue."));
            }

            // Checks if the authenticated user is the seller of the item
            if (item.getSellerId() == authUser) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You cannot place a bid on your own auction item."));
            }

            PlaceBidResponse response = auctionService.placeBid(
                    authUser,
                    authUsername,
                    catalogueId,
                    bidRequest.getAmount()
            );

            if (response.getSuccess()) {
                biddingSessionManager.setItem(session, item.getId());
                return ResponseEntity.ok(Map.of(
                        "message", response.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{catalogueId}/auctionend")
    public ResponseEntity<?> getAuctionEnd(@PathVariable int catalogueId) {
        try {
            GetAuctionEndResponse response = auctionService.getAuctionEnd(catalogueId);

            if (response.getFound()) {
                EndTimeResponseDto dto = AuctionServiceDtoMapper.fromProto(response);
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{catalogueId}/auctionstatus")
    public ResponseEntity<?> getAuctionStatus(@PathVariable int catalogueId) {
        try {
            GetAuctionStatusResponse response = auctionService.getAuctionStatus(catalogueId);

            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "highest bidder", response.getHighestBidder(),
                        "current highest bid", response.getCurrentAmount(),
                        "remaining time", response.getRemainingTime(),
                        "auction status", response.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{catalogueId}/auctionwinner")
    public ResponseEntity<?> getAuctionWinner(@PathVariable int catalogueId) {
        try {
            GetAuctionWinnerResponse response = auctionService.getAuctionWinner(catalogueId);

            if (response.getFound()) {
                return ResponseEntity.ok(Map.of(
                        "winning user", response.getWinnerUserId(),
                        "final price", response.getFinalPrice(),
                        "auction status", response.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", response.getMessage()));
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
