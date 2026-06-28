package com.nexuswms.inventory.controller;

import com.nexuswms.inventory.dto.request.StockAdjustmentRequest;
import com.nexuswms.inventory.dto.response.SkuLocationResponse;
import com.nexuswms.inventory.dto.response.StockMovementResponse;
import com.nexuswms.inventory.dto.response.StockSummaryResponse;
import com.nexuswms.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v{version}/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /* -------------------------------------------------------------------------
     * GET /stock
     * Returns stock summary for all SKUs — total, reserved, available quantities
     * and whether each SKU is below its reorder point.
     * Restricted to roles that manage or oversee inventory.
     * ------------------------------------------------------------------------- */
    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<List<StockSummaryResponse>> getStockSummary() {
        return ResponseEntity.ok(stockService.getStockSummary());
    }

    /* -------------------------------------------------------------------------
     * GET /stock/{skuId}/locations
     * Returns all shelf locations for a specific SKU with per-location
     * quantities, batch IDs, and expiry dates.
     * RECEIVER included — needed to know where to put inbound goods.
     * ------------------------------------------------------------------------- */
    @GetMapping(value = "/{skuId}/locations", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER')")
    public ResponseEntity<List<SkuLocationResponse>> getSkuLocations(@PathVariable UUID skuId) {
        return ResponseEntity.ok(stockService.getSkuLocations(skuId));
    }

    /* -------------------------------------------------------------------------
     * POST /stock/adjust
     * Manual stock adjustment — positive to increase, negative to decrease.
     * Worker identity taken from the JWT via Authentication injection —
     * cleaner than SecurityContextHolder and easier to test.
     * Restricted to ADMIN and INVENTORY_CONTROLLER — deliberate manual changes only.
     * ------------------------------------------------------------------------- */
    @PostMapping(value = "/adjust", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<SkuLocationResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(stockService.adjustStock(request, authentication.getName()));
    }

    /* -------------------------------------------------------------------------
     * GET /stock/alerts/reorder
     * Returns all SKUs with an open reorder alert — stock at or below
     * their configured reorder point. Used by managers and inventory
     * controllers to trigger new purchase orders.
     * ------------------------------------------------------------------------- */
    @GetMapping(value = "/alerts/reorder", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<List<StockSummaryResponse>> getReorderAlerts() {
        return ResponseEntity.ok(stockService.getReorderAlerts());
    }

    /* -------------------------------------------------------------------------
     * GET /stock/{skuId}/movements
     * Returns the full audit trail of stock movements for a SKU, newest first.
     * Includes both manual adjustments (type: ADJUSTMENT) and inbound
     * goods receipts (type: GOODS_RECEIPT).
     * ------------------------------------------------------------------------- */
    @GetMapping(value = "/{skuId}/movements", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements(@PathVariable UUID skuId) {
        return ResponseEntity.ok(stockService.getStockMovements(skuId));
    }
}