package com.cash.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import com.cash.dtos.CatalogueItemRequestDto;
import com.cash.dtos.CatalogueItemResponseDto;
import com.cash.grpc.catalogue.*;
import com.cash.mappers.CatalogueServiceDtoMapper;
import com.cash.services.CatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogue")
@Tag(name = "Catalogue", description = "Catalogue item management")
public class CatalogueController {

    private final CatalogueService catalogueService;

    @Autowired
    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @Operation(summary = "Get all catalogue items", description = "Fetches all items in the catalogue")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all items", content = @Content(schema = @Schema(implementation = CollectionModel.class)))
    @GetMapping("/items")
    public ResponseEntity<CollectionModel<CatalogueItemResponseDto>> getAllItems() {
        ItemList response = catalogueService.getAllItems();
        List<CatalogueItemResponseDto> items = CatalogueServiceDtoMapper.fromProtoList(response.getItemsList());

        // Add HATEOAS links to each item
        items = items.stream().map(this::addLinksToItem).collect(Collectors.toList());

        // Create CollectionModel with links
        CollectionModel<CatalogueItemResponseDto> collectionModel = CollectionModel.of(items);
        collectionModel.add(linkTo(methodOn(CatalogueController.class).getAllItems()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    @Operation(summary = "Search catalogue items", description = "Search for items by keyword in title")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matching items", content = @Content(schema = @Schema(implementation = CollectionModel.class)))
    @GetMapping("/search")
    public ResponseEntity<CollectionModel<CatalogueItemResponseDto>> searchItems(
            @Parameter(description = "Search keyword to filter items", required = true) @RequestParam String keyword) {
        ItemList response = catalogueService.searchItems(keyword);
        List<CatalogueItemResponseDto> items = CatalogueServiceDtoMapper.fromProtoList(response.getItemsList());

        // Add HATEOAS links to each item
        items = items.stream().map(this::addLinksToItem).collect(Collectors.toList());

        // Create CollectionModel with links
        CollectionModel<CatalogueItemResponseDto> collectionModel = CollectionModel.of(items);
        collectionModel.add(
                linkTo(methodOn(CatalogueController.class).searchItems(keyword)).withSelfRel());
        collectionModel.add(
                linkTo(methodOn(CatalogueController.class).getAllItems()).withRel("all-items"));

        return ResponseEntity.ok(collectionModel);
    }

    @Operation(summary = "Create new catalogue item", description = "Creates a new item in the catalogue")
    @ApiResponse(responseCode = "201", description = "Item successfully created", content = @Content(schema = @Schema(implementation = CatalogueItemResponseDto.class)))
    @PostMapping("/items")
    public ResponseEntity<CatalogueItemResponseDto> createItem(
            @Parameter(description = "Catalogue item details", required = true) @RequestBody CatalogueItemRequestDto dto) {
        CreateItemRequest request = CatalogueServiceDtoMapper.toProto(dto);
        ItemResponse response = catalogueService.createItem(request);
        CatalogueItemResponseDto item = CatalogueServiceDtoMapper.fromProto(response);

        // Add HATEOAS links
        addLinksToItem(item);

        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @Operation(summary = "Get catalogue item by ID", description = "Fetches a single catalogue item with HATEOAS links")
    @ApiResponse(responseCode = "200", description = "Item retrieved successfully", content = @Content(schema = @Schema(implementation = CatalogueItemResponseDto.class)))
    @GetMapping("/items/{id}")
    public ResponseEntity<CatalogueItemResponseDto> getItem(
            @Parameter(description = "Item ID", required = true) @PathVariable int id) {
        ItemResponse response = catalogueService.getItem(id);
        CatalogueItemResponseDto item = CatalogueServiceDtoMapper.fromProto(response);

        // Add HATEOAS links
        addLinksToItem(item);

        return ResponseEntity.ok(item);
    }

    /** Helper method to add HATEOAS links to a catalogue item */
    private CatalogueItemResponseDto addLinksToItem(CatalogueItemResponseDto item) {
        // Add self link
        item.add(linkTo(methodOn(CatalogueController.class).getItem(item.getId())).withSelfRel());

        // Add link to all items
        item.add(linkTo(methodOn(CatalogueController.class).getAllItems()).withRel("catalogue"));

        // Add conditional links based on item state
        if (item.isActive()) {
            // If item is active, add auction-related links
            item.add(
                    linkTo(methodOn(AuctionController.class).getAuctionStatus(item.getId()))
                            .withRel("auction-status"));
            item.add(
                    linkTo(methodOn(AuctionController.class).placeBid(item.getId(), null, null, null))
                            .withRel("place-bid"));
        } else {
            // If not active, might show link to start auction (if user is seller)
            item.add(
                    linkTo(methodOn(AuctionController.class).startAuction(item.getId(), null))
                            .withRel("start-auction"));
        }

        return item;
    }
}
