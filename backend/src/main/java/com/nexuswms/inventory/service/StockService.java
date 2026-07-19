package com.nexuswms.inventory.service;

import com.nexuswms.common.exception.InsufficientStockException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEvent;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.inventory.dto.request.StockAdjustmentRequest;
import com.nexuswms.inventory.dto.response.SkuLocationInfoResponse;
import com.nexuswms.inventory.dto.response.SkuLocationResponse;
import com.nexuswms.inventory.dto.response.StockMovementResponse;
import com.nexuswms.inventory.dto.response.StockSummaryResponse;
import com.nexuswms.inventory.entity.ReorderAlert;
import com.nexuswms.inventory.entity.Shelf;
import com.nexuswms.inventory.entity.Sku;
import com.nexuswms.inventory.entity.SkuLocation;
import com.nexuswms.inventory.entity.StockMovement;
import com.nexuswms.inventory.entity.Zone;
import com.nexuswms.inventory.repository.ReorderAlertRepository;
import com.nexuswms.inventory.repository.ShelfRepository;
import com.nexuswms.inventory.repository.SkuLocationRepository;
import com.nexuswms.inventory.repository.SkuRepository;
import com.nexuswms.inventory.repository.StockMovementRepository;
import com.nexuswms.user.entity.User;
import com.nexuswms.user.repository.UserRepository;
import com.nexuswms.inventory.repository.ZoneRepository; 

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {

    private final SkuRepository skuRepository;
    private final ShelfRepository shelfRepository;
    private final SkuLocationRepository skuLocationRepository;
    private final ReorderAlertRepository reorderAlertRepository;
    private final UserRepository userRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseEventPublisher eventPublisher; 
    private final ZoneRepository zoneRepository;
    /* -------------------------------------------------------------------------
     * GET /stock
     * Returns a stock summary for every SKU in the system.
     * total = all units across all shelf locations for this SKU.
     * reserved = units locked for open orders, not available for new picks.
     * available = total - reserved.
     * needsReorder = total <= reorderPoint.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<StockSummaryResponse> getStockSummary() {
        return skuRepository.findAll().stream().map(sku -> {
            Integer total = skuLocationRepository.getTotalQuantityBySkuId(sku.getId());
            Integer available = skuLocationRepository.getAvailableQuantityBySkuId(sku.getId());
            Integer reserved = total - available;
            return new StockSummaryResponse(
                    sku.getId(), sku.getSkuCode(), sku.getName(),
                    total, reserved, available,
                    sku.getReorderPoint(),
                    total <= sku.getReorderPoint());
        }).toList();
    }

    /* -------------------------------------------------------------------------
     * GET /stock/{skuId}/locations
     * Returns all shelf locations where this SKU has stock,
     * including quantity, reserved quantity, batch ID and expiry date.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<SkuLocationResponse> getSkuLocations(UUID skuId) {
        return skuLocationRepository.findBySkuId(skuId).stream()
                .map(SkuLocationResponse::from)
                .toList();
    }

    /* -------------------------------------------------------------------------
     * POST /stock/adjust
     * Manual stock adjustment — positive to increase, negative to decrease.
     * Zero quantity is rejected at this layer regardless of DTO validation.
     * Cannot reduce below zero or below reserved quantity.
     * Every adjustment writes a StockMovement audit record.
     * Triggers a ReorderAlert if total stock drops to or below reorder point
     * and no open alert already exists for this SKU.
     * ------------------------------------------------------------------------- */
    @Transactional
    public SkuLocationResponse adjustStock(StockAdjustmentRequest request, String workerId) {
        if (request.quantity() == 0) {
            throw new IllegalArgumentException("Adjustment quantity cannot be zero");
        }

        Sku sku = skuRepository.findById(request.skuId())
                .orElseThrow(() -> new ResourceNotFoundException("SKU", request.skuId().toString()));

        Shelf shelf = shelfRepository.findById(request.shelfId())
                .orElseThrow(() -> new ResourceNotFoundException("Shelf", request.shelfId().toString()));

        User worker = userRepository.findById(UUID.fromString(workerId))
                .orElseThrow(() -> new ResourceNotFoundException("User", workerId));

        SkuLocation location = skuLocationRepository
                .findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), request.batchId())
                .orElse(SkuLocation.builder()
                        .sku(sku)
                        .shelf(shelf)
                        .batchId(request.batchId())
                        .build());

        int newQuantity = location.getQuantity() + request.quantity();

        if (newQuantity < 0) {
            throw new InsufficientStockException(sku.getSkuCode(), shelf.getCode());
        }
        if (newQuantity < location.getReservedQuantity()) {
            throw new InsufficientStockException(sku.getSkuCode(), shelf.getCode());
        }

        location.setQuantity(newQuantity);
        skuLocationRepository.save(location);

        updateZoneOccupancy(shelf, request.quantity());

        stockMovementRepository.save(StockMovement.builder()
                .sku(sku)
                .shelf(shelf)
                .quantity(request.quantity())
                .movementType("ADJUSTMENT")
                .worker(worker)
                .batchId(request.batchId())
                .notes(request.reason())
                .build()); 

        if (request.quantity() < 0) {
            checkAndTriggerReorderAlert(sku);
        } else {
            checkAndResolveReorderAlert(sku);
        }

        return SkuLocationResponse.from(location);
    }

    /* -------------------------------------------------------------------------
     * Helper method to evaluate stock level against reorder point.
     * Creates an OPEN ReorderAlert and publishes a REORDER_ALERT warehouse event
     * if total stock is at or below the SKU's reorder point and no open alert
     * currently exists for this SKU.
     * ------------------------------------------------------------------------- */
    private void checkAndTriggerReorderAlert(Sku sku) {
        Integer total = skuLocationRepository.getTotalQuantityBySkuId(sku.getId());
        
        if (total <= sku.getReorderPoint()) {
            if (!reorderAlertRepository.existsBySkuIdAndStatus(sku.getId(), "OPEN")) {
                reorderAlertRepository.save(ReorderAlert.builder()
                        .sku(sku)
                        .currentQuantity(total)
                        .reorderPoint(sku.getReorderPoint())
                        .status("OPEN") // Explicitly set status , it's already defaulted to "OPEN" in the entity, but being explicit here for clarity
                        .build());

                eventPublisher.publishAlert(WarehouseEvent.of(
                    "REORDER_ALERT", sku.getSkuCode(), "OPEN", "SYSTEM", "INVENTORY"
                ));
            }
        }
    }

    /* -------------------------------------------------------------------------
     * Helper method to auto-resolve open reorder alerts upon stock replenishment.
     * Checks if total stock has recovered above the SKU's reorder point and
     * updates any existing OPEN alert to RESOLVED status while publishing an event.
     * ------------------------------------------------------------------------- */
    private void checkAndResolveReorderAlert(Sku sku) {
        Integer total = skuLocationRepository.getTotalQuantityBySkuId(sku.getId());

        if (total != null && total > sku.getReorderPoint()) {
            reorderAlertRepository.findBySkuIdAndStatus(sku.getId(), "OPEN")
                    .ifPresent(alert -> {
                        alert.setStatus("RESOLVED");
                        reorderAlertRepository.save(alert);

                        eventPublisher.publishAlert(WarehouseEvent.of(
                                "REORDER_ALERT",
                                sku.getSkuCode(),
                                "RESOLVED",
                                "SYSTEM",
                                "INVENTORY"
                        ));
                    });
        }
    }

    /* -------------------------------------------------------------------------
     * GET /stock/alerts/reorder
     * Returns all SKUs that have an open reorder alert.
     * Alerts are created automatically on every stock adjustment when
     * total stock drops to or below the SKU's configured reorder point.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<StockSummaryResponse> getReorderAlerts() {
        return reorderAlertRepository.findByStatus("OPEN").stream().map(alert -> {
            Sku sku = alert.getSku();
            Integer total = skuLocationRepository.getTotalQuantityBySkuId(sku.getId());
            Integer available = skuLocationRepository.getAvailableQuantityBySkuId(sku.getId());
            return new StockSummaryResponse(
                    sku.getId(), sku.getSkuCode(), sku.getName(),
                    total, total - available, available,
                    sku.getReorderPoint(), true);
        }).toList();
    }

    /* -------------------------------------------------------------------------
     * GET /stock/{skuId}/movements
     * Returns the full stock movement audit trail for a SKU, newest first.
     * Includes both manual adjustments and goods receipt inbound movements.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getStockMovements(UUID skuId) {
        skuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("SKU", skuId.toString()));
        return stockMovementRepository.findBySkuIdOrderByMovedAtDesc(skuId).stream()
                .map(StockMovementResponse::from)
                .toList();
    }

    /* -------------------------------------------------------------------------
     * Called by GoodsReceiptService when goods arrive against a purchase order.
     * Finds existing SkuLocation for this SKU + shelf + batch combination,
     * or creates a new one if this is the first receipt for this batch.
     * SKU and Shelf are fetched once upfront and reused for both the location
     * and the movement — avoids duplicate DB queries.
     * worker is nullable — procurement receivedBy UUID may not match a system user.
     * Writes a StockMovement with type GOODS_RECEIPT for the audit trail.
     * ------------------------------------------------------------------------- */
    @Transactional
    public void adjustFromGoodsReceipt(UUID skuId, UUID shelfId, int quantity,
                                        String batchId, LocalDate expiryDate,
                                        UUID workerId, String notes) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found: " + skuId));
        Shelf shelf = shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelf not found: " + shelfId));
        User worker = userRepository.findById(workerId).orElse(null);

        SkuLocation location = skuLocationRepository
                .findBySkuIdAndShelfIdAndBatchId(skuId, shelfId, batchId)
                .orElseGet(() -> SkuLocation.builder()
                        .sku(sku)
                        .shelf(shelf)
                        .quantity(0)
                        .reservedQuantity(0)
                        .batchId(batchId)
                        .expiryDate(expiryDate)
                        .build());

        location.setQuantity(location.getQuantity() + quantity);
        skuLocationRepository.save(location);
        updateZoneOccupancy(shelf, quantity);
        checkAndResolveReorderAlert(sku);                                    
        stockMovementRepository.save(StockMovement.builder()
                .sku(sku)
                .shelf(shelf)
                .quantity(quantity)
                .movementType("GOODS_RECEIPT")
                .referenceType("GOODS_RECEIPT_LINE")
                .worker(worker)
                .batchId(batchId)
                .notes(notes)
                .build());
    }

    /* -------------------------------------------------------------------------
        * Helper method to keep Zone.currentOccupancy in sync with actual stock.
        * Called from adjustStock() and adjustFromGoodsReceipt() — the two places
        * where a shelf's stock quantity changes. Walks shelf -> aisle -> zone and
        * applies the same signed delta that was just applied to the SkuLocation,
        * in the same transaction, so occupancy can never drift out of sync with
        * a partially-completed adjustment.
        * ------------------------------------------------------------------------- */
        private void updateZoneOccupancy(Shelf shelf, int delta) {
        Zone zone = shelf.getAisle().getZone();
        zone.setCurrentOccupancy(zone.getCurrentOccupancy() + delta);
        zoneRepository.save(zone);
        }

    /* ----- Find Available Location For SKU ----- */
        /**
         * Finds the first stock location for a SKU that has enough available quantity
         * to satisfy the requested amount. Used by PickListService for pick list generation
         * without violating bounded context — inventory repositories stay inside this package.
         *
         * @param skuId            the SKU UUID to search locations for
         * @param quantityRequired the minimum available quantity needed
         * @return a SkuLocationInfoResponse with shelf and batch details
         * @throws IllegalArgumentException if no location has sufficient available stock
         */
        public SkuLocationInfoResponse findAvailableLocationForSku(UUID skuId, int quantityRequired) {
        return skuLocationRepository.findBySkuId(skuId).stream()
                .filter(loc -> (loc.getQuantity() - loc.getReservedQuantity()) >= quantityRequired)
                .findFirst()
                .map(loc -> {
                        Shelf shelf = shelfRepository.findById(loc.getShelf().getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Shelf not found for sku location"));
                        return new SkuLocationInfoResponse(
                                shelf.getId(),
                                shelf.getCode(),
                                loc.getBatchId(),
                                loc.getQuantity() - loc.getReservedQuantity()
                        );
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        "No available stock location for SKU: " + skuId));
        }
}