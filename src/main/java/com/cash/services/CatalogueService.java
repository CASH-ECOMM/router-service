package com.cash.services;

import com.cash.grpc.catalogue.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;

@Service
public class CatalogueService {

    private final CatalogueServiceGrpc.CatalogueServiceBlockingStub blockingStub;

    public CatalogueService() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        blockingStub = CatalogueServiceGrpc.newBlockingStub(channel);
    }

    public ItemList getAllItems() {
        return blockingStub.getAllItems(Empty.newBuilder().build());
    }

    public ItemList searchItems(String keyword) {
        return blockingStub.searchItems(SearchRequest.newBuilder().setKeyword(keyword).build());
    }

    public ItemResponse createItem(CreateItemRequest request) {
        return blockingStub.createItem(request);
    }
    public ItemResponse getItem(int id) {
        GetItemRequest request = GetItemRequest.newBuilder()
            .setId(id)
            .build();
     return blockingStub.getItem(request);
}

}
