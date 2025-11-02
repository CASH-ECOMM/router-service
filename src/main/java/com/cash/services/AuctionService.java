package com.cash.services;

import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.cash.grpc.auctionservice.AuctionServiceGrpc;
import com.cash.grpc.auctionservice.*;
import com.google.protobuf.Timestamp;

@Service
public class AuctionService {

    @GrpcClient("auction-service")
    private AuctionServiceGrpc.AuctionServiceBlockingStub auctionServiceStub;

    public StartAuctionResponse startAuction(int userId, int catalogueId, int startingAmount, Timestamp endTime){
        StartAuctionRequest request = StartAuctionRequest.newBuilder()
                .setUserId(userId)
                .setCatalogueId(catalogueId)
                .setStartingAmount(startingAmount)
                .setEndTime(endTime)
                .build();
        return auctionServiceStub.startAuction(request);
    }

    public PlaceBidResponse placeBid(int userId, String username, int catalogueId, int bidAmount){
        PlaceBidRequest request = PlaceBidRequest.newBuilder()
                .setUserId(userId)
                .setUsername(username)
                .setCatalogueId(catalogueId)
                .setAmount(bidAmount)
                .build();
        return auctionServiceStub.placeBid(request);
    }

    public GetAuctionEndResponse getAuctionEnd(int catalogueId){
        GetAuctionEndRequest request = GetAuctionEndRequest.newBuilder()
                .setCatalogueId(catalogueId)
                .build();
        return auctionServiceStub.getAuctionEnd(request);
    }

    public GetAuctionStatusResponse getAuctionStatus(int catalogueId){
        GetAuctionStatusRequest request = GetAuctionStatusRequest.newBuilder()
                .setCatalogueId(catalogueId)
                .build();
        return auctionServiceStub.getAuctionStatus(request);
    }

    public GetAuctionWinnerResponse getAuctionWinner(int catalogueId){
        GetAuctionWinnerRequest request = GetAuctionWinnerRequest.newBuilder()
                .setCatalogueId(catalogueId)
                .build();
        return auctionServiceStub.getAuctionWinner(request);
    }
}
