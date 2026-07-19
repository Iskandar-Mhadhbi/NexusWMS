package com.nexuswms.inventory.controller;

import com.nexuswms.inventory.dto.request.CategoryRequest;
import com.nexuswms.inventory.dto.request.SkuRequest;
import com.nexuswms.inventory.dto.response.CategoryResponse;
import com.nexuswms.inventory.dto.response.SkuResponse;
import com.nexuswms.inventory.service.SkuService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller managing Stock Keeping Units (SKUs) and product categories.
 * Core foundational module for defining item master data used across receiving, storage, and picking.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/skus")
@Tag(name = "Skus", description = "SKU management")
public class SkuController {

    private final SkuService skuService;
 

    /**
     * Creates a new material or product category.
     * * @param request Contains the new category definitions (e.g., name, parent classification ID).
     * @return The created CategoryResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "201", description = "Category successfully created with the provided details")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skuService.createCategory(request));
    }

    /**
     * Retrieves the entire hierarchical category tree structure.
     * * @return A list of all CategoryResponse items wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "200", description = "Hierarchical list of all categories retrieved successfully")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(skuService.getAllCategories());
    }

    /**
     * Retrieves top-level ("root") categories that have no parents.
     * Used for initializing primary navigation filters in inventory setups.
     * * @return A list of root CategoryResponse items wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/categories/roots")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "200", description = "List of root (top-level) categories retrieved successfully")
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        return ResponseEntity.ok(skuService.getRootCategories());
    }

    /**
     * Defines and records a new SKU (Stock Keeping Unit) in the master catalog.
     * * @param request Contains specific item criteria like code, dimensions, weights, and base units.
     * @return The newly registered SkuResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "201", description = "SKU successfully created and registered in the master catalog")
    public ResponseEntity<SkuResponse> createSku(@Valid @RequestBody SkuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skuService.createSku(request));
    }

    /**
     * Fetches SKUs from the master catalog. 
     * Supports optional inline string query tracking to filter by code, description, or attributes.
     * * @param search Optional alphanumeric text string to search against master SKU registries.
     * @return Filtered or full list of SkuResponses wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER', 'PICKER')")
    @ApiResponse(responseCode = "200", description = "List of SKUs retrieved successfully (filtered if search parameter provided)")
    public ResponseEntity<List<SkuResponse>> getAllSkus(
            @RequestParam(required = false) String search) {
        // Evaluate if a functional search term was provided to dispatch targeted query routing
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(skuService.searchSkus(search));
        }
        return ResponseEntity.ok(skuService.getAllSkus());
    }

    /**
     * Resolves a distinct catalog item using its primary system UUID.
     * * @param id Unique internal UUID of the SKU record.
     * @return The matched SkuResponse record wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER', 'PICKER')")
    @ApiResponse(responseCode = "200", description = "SKU details retrieved successfully by ID")
    public ResponseEntity<SkuResponse> getSkuById(@PathVariable UUID id) {
        return ResponseEntity.ok(skuService.getSkuById(id));
    }

    /**
     * Resolves a distinct catalog item by its human-readable barcode or business code.
     * Primarily used for instant processing during RF scanner lookups at warehouse docks.
     * * @param skuCode Unique business string barcode key identifier (e.g., "SKU-BA-123").
     * @return The matched SkuResponse record wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/code/{skuCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER', 'PICKER')")
    @ApiResponse(responseCode = "200", description = "SKU details retrieved successfully by business code")
    public ResponseEntity<SkuResponse> getSkuByCode(@PathVariable String skuCode) {
        return ResponseEntity.ok(skuService.getSkuByCode(skuCode));
    }
}