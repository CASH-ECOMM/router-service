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

    @PostMapping("/startauction")
    public ResponseEntity<?> startAuction(@RequestBody StartAuctionRequestDto dto, HttpServletRequest request) {
        try {
            Integer authUser = AuthenticatedUser.getUserId(request);
            if (authUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "You need to be logged in to start an auction."));
            }

            StartAuctionRequest auctionRequest = AuctionServiceDtoMapper.toProto(dto);

            // Gets the item from the catalogue service
            //ItemResponse item = catalogueService.getItem(auctionRequest.getCatalogueId());

            StartAuctionResponse response = auctionService.startAuction(
                    authUser,
                    auctionRequest.getCatalogueId(),
                    auctionRequest.getStartingAmount(),
                    auctionRequest.getEndTime()
                    //item.getEndTime()     // This gets the end time for the item from the catalogue service (Needs to be converted from String to Timestamp)
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

    @PostMapping("/placebid")
    public ResponseEntity<?> placeBid(@RequestBody PlaceBidRequestDto dto, HttpServletRequest request, HttpSession session) {
        try {
            Integer authUser = AuthenticatedUser.getUserId(request);
            String authUsername = AuthenticatedUser.getUsername(request);
            Integer currentItemBid = biddingSessionManager.getItem(session);

            if (authUser == null) {  // Need to implement a way for a user to not bid on their own auction
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You need to be logged in to place a bid."));
            }
            PlaceBidRequest bidRequest = AuctionServiceDtoMapper.toProto(dto);

            if (currentItemBid != null && currentItemBid.equals(bidRequest.getCatalogueId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only bid on one auction item at a time. Please finish bidding on your current item before placing a bid on another."));
            }

            ItemResponse item = catalogueService.getItem(bidRequest.getCatalogueId());

            // Checks if the authenticated user is the seller of the item
            if (item.getSellerId() == authUser) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You cannot place a bid on your own auction item."));
            }

            PlaceBidResponse response = auctionService.placeBid(
                    authUser,
                    authUsername,
                    bidRequest.getCatalogueId(),
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

    @GetMapping("/auctionend/{catalogueId}")
    public ResponseEntity<?> getAuctionEnd(@PathVariable int catalogueId) {
        try {
            GetAuctionEndResponse response = auctionService.getAuctionEnd(catalogueId);

            if (response.getFound()) {
                EndTimeResponseDto dto = AuctionServiceDtoMapper.fromProto(response);
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/auctionstatus/{catalogueId}")
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

    @GetMapping("/auctionwinner/{catalogueId}")
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
