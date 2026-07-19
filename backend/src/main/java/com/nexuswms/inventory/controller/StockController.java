package com.nexuswms.inventory.controller;

import com.nexuswms.inventory.dto.request.StockAdjustmentRequest;
import com.nexuswms.inventory.dto.response.SkuLocationResponse;
import com.nexuswms.inventory.dto.response.StockMovementResponse;
import com.nexuswms.inventory.dto.response.StockSummaryResponse;
import com.nexuswms.inventory.service.StockService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing inventory stock levels, locations, adjustments,
 * and related alerts. Handles real-time stock visibility and movements.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/stock")
@Tag(name = "stocks", description = "Manage the stocks in the inventory ")
public class StockController {

    private final StockService stockService; 
    /**
     * Returns stock summary for all SKUs — total, reserved, available quantities
     * and whether each SKU is below its reorder point.
     *
     * @return List of StockSummaryResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "200", description = "Stock summary for all SKUs retrieved successfully")
    public ResponseEntity<List<StockSummaryResponse>> getStockSummary() {
        return ResponseEntity.ok(stockService.getStockSummary());
    }

    /**
     * Returns all shelf locations for a specific SKU with per-location
     * quantities, batch IDs, and expiry dates.
     *
     * @param skuId UUID of the SKU
     * @return List of SkuLocationResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/{skuId}/locations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER')")
    @ApiResponse(responseCode = "200", description = "Stock locations for the SKU retrieved successfully")
    public ResponseEntity<List<SkuLocationResponse>> getSkuLocations(@PathVariable UUID skuId) {
        return ResponseEntity.ok(stockService.getSkuLocations(skuId));
    }

    /**
     * Manual stock adjustment — positive to increase, negative to decrease.
     * Worker identity is taken from the JWT via Authentication.
     *
     * @param request Stock adjustment details
     * @param authentication Current authenticated user
     * @return Updated SkuLocationResponse wrapped in a 200 OK ResponseEntity.
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "200", description = "Stock adjusted successfully")
    public ResponseEntity<SkuLocationResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(stockService.adjustStock(request, authentication.getName()));
    }

    /**
     * Returns all SKUs with an open reorder alert — stock at or below
     * their configured reorder point.
     *
     * @return List of StockSummaryResponse (reorder alerts) wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/alerts/reorder")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "200", description = "Reorder alerts retrieved successfully")
    public ResponseEntity<List<StockSummaryResponse>> getReorderAlerts() {
        return ResponseEntity.ok(stockService.getReorderAlerts());
    }

    /**
     * Returns the full audit trail of stock movements for a SKU, newest first.
     * Includes both manual adjustments and inbound goods receipts.
     *
     * @param skuId UUID of the SKU
     * @return List of StockMovementResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/{skuId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "200", description = "Stock movement history retrieved successfully")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements(@PathVariable UUID skuId) {
        return ResponseEntity.ok(stockService.getStockMovements(skuId));
    }
}