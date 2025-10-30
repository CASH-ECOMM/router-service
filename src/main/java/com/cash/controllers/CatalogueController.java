package com.cash.controllers;

import com.cash.dtos.CatalogueItemRequestDto;
import com.cash.dtos.CatalogueItemResponseDto;
import com.cash.mappers.CatalogueServiceDtoMapper;
import com.cash.services.CatalogueService;
import com.cash.grpc.catalogue.*;


import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {

    private final CatalogueService catalogueService;

    @Autowired
    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping("/items")
    public ResponseEntity<?> getAllItems() {
        try {
            ItemList response = catalogueService.getAllItems();
            List<CatalogueItemResponseDto> items = CatalogueServiceDtoMapper.fromProtoList(response.getItemsList());
            return ResponseEntity.ok(items);
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchItems(@RequestParam String keyword) {
        try {
            ItemList response = catalogueService.searchItems(keyword);
            List<CatalogueItemResponseDto> items = CatalogueServiceDtoMapper.fromProtoList(response.getItemsList());
            return ResponseEntity.ok(items);
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/items")
    public ResponseEntity<?> createItem(@RequestBody CatalogueItemRequestDto dto) {
        try {
            CreateItemRequest request = CatalogueServiceDtoMapper.toProto(dto);
            ItemResponse response = catalogueService.createItem(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CatalogueServiceDtoMapper.fromProto(response));
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<?> getItem(@PathVariable int id) {
        try {
            ItemResponse response = catalogueService.getItem(id);
            CatalogueItemResponseDto item = CatalogueServiceDtoMapper.fromProto(response);
            return ResponseEntity.ok(item);
        } catch (StatusRuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
}
