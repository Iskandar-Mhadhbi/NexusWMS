package com.nexuswms.inventory.controller;

import com.nexuswms.inventory.dto.request.AisleRequest;
import com.nexuswms.inventory.dto.request.ShelfRequest;
import com.nexuswms.inventory.dto.request.ZoneRequest;
import com.nexuswms.inventory.dto.response.AisleResponse;
import com.nexuswms.inventory.dto.response.ShelfResponse;
import com.nexuswms.inventory.dto.response.ZoneResponse;
import com.nexuswms.inventory.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v{version}/zones")
public class ZoneController {

    private final LocationService locationService;

    public ZoneController(LocationService locationService) {
        this.locationService = locationService;
    }

    /* -------------------------------------------------------------------------
     * POST /zones
     * Creates a new warehouse zone (e.g. RECEIVING, STORAGE-A, DISPATCH).
     * Restricted to ADMIN and INVENTORY_CONTROLLER — physical layout changes
     * require elevated permissions.
     * ------------------------------------------------------------------------- */
    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<ZoneResponse> createZone(@Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createZone(request));
    }

    /* -------------------------------------------------------------------------
     * GET /zones
     * Returns all warehouse zones.
     * RECEIVER included — needs zone list to assign shelf locations on inbound.
     * ------------------------------------------------------------------------- */
    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER')")
    public ResponseEntity<List<ZoneResponse>> getAllZones() {
        return ResponseEntity.ok(locationService.getAllZones());
    }

    /* -------------------------------------------------------------------------
     * GET /zones/{id}
     * Returns a single zone by UUID with its capacity and occupancy info.
     * ------------------------------------------------------------------------- */
    @GetMapping(value = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER')")
    public ResponseEntity<ZoneResponse> getZoneById(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getZoneById(id));
    }

    /* -------------------------------------------------------------------------
     * POST /zones/aisles
     * Creates a new aisle within a zone.
     * Aisle code must be unique within its zone (enforced at service layer).
     * ------------------------------------------------------------------------- */
    @PostMapping(value = "/aisles", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<AisleResponse> createAisle(@Valid @RequestBody AisleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createAisle(request));
    }

    /* -------------------------------------------------------------------------
     * GET /zones/{zoneId}/aisles
     * Returns all aisles within a zone.
     * PICKER included — needs aisle layout to navigate pick lists.
     * ------------------------------------------------------------------------- */
    @GetMapping(value = "/{zoneId}/aisles", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER', 'PICKER')")
    public ResponseEntity<List<AisleResponse>> getAislesByZone(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(locationService.getAislesByZone(zoneId));
    }

    /* -------------------------------------------------------------------------
     * POST /zones/shelves
     * Creates a new shelf within an aisle.
     * Shelf code, max weight and level are set at creation and do not change.
     * ------------------------------------------------------------------------- */
    @PostMapping(value = "/shelves", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<ShelfResponse> createShelf(@Valid @RequestBody ShelfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createShelf(request));
    }

    /* -------------------------------------------------------------------------
     * GET /zones/{zoneId}/shelves
     * Returns all shelves within a zone across all its aisles.
     * PICKER and RECEIVER included — both need shelf codes during operations.
     * ------------------------------------------------------------------------- */
    @GetMapping(value = "/{zoneId}/shelves", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER', 'PICKER')")
    public ResponseEntity<List<ShelfResponse>> getShelvesByZone(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(locationService.getShelvesByZone(zoneId));
    }
}