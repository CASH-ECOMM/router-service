package com.cash.services;

import com.cash.grpc.catalogue.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class CatalogueService {

    @GrpcClient("catalogue-service")
    private CatalogueServiceGrpc.CatalogueServiceBlockingStub blockingStub;

    public ItemList getAllItems() {
        return blockingStub.getAllItems(Empty.newBuilder().build());
    }

    public ItemList searchItems(String keyword) {
        return blockingStub.searchItems(SearchRequest.newBuilder().setKeyword(keyword).build());
    }

    public ItemResponse createItem(CreateItemRequest request) {
        return blockingStub.createItem(request);
    }
}
