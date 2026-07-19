package com.nexuswms.inventory.controller;

import com.nexuswms.inventory.dto.request.AisleRequest;
import com.nexuswms.inventory.dto.request.ShelfRequest;
import com.nexuswms.inventory.dto.request.ZoneRequest;
import com.nexuswms.inventory.dto.response.AisleResponse;
import com.nexuswms.inventory.dto.response.ShelfResponse;
import com.nexuswms.inventory.dto.response.ZoneResponse;
import com.nexuswms.inventory.service.LocationService;
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
 * REST Controller for managing warehouse zones, aisles, and shelves.
 * Handles the physical layout structure of the warehouse.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/zones")
@Tag(name = "Zones", description = "Warehouse zone management")
public class ZoneController {

    private final LocationService locationService; 

    /**
     * Creates a new warehouse zone (e.g. RECEIVING, STORAGE-A, DISPATCH).
     *
     * @param request Zone details
     * @return The created ZoneResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping( )
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "201", description = "Warehouse zone successfully created")
    public ResponseEntity<ZoneResponse> createZone(@Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createZone(request));
    }

    /**
     * Returns all warehouse zones.
     *
     * @return List of all ZoneResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping( )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER')")
    @ApiResponse(responseCode = "200", description = "List of all warehouse zones retrieved successfully")
    public ResponseEntity<List<ZoneResponse>> getAllZones() {
        return ResponseEntity.ok(locationService.getAllZones());
    }

    /**
     * Returns a single zone by UUID with its capacity and occupancy info.
     *
     * @param id UUID of the zone
     * @return The ZoneResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping ("/{id}" )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER')")
    @ApiResponse(responseCode = "200", description = "Zone details retrieved successfully by ID")
    public ResponseEntity<ZoneResponse> getZoneById(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getZoneById(id));
    }

    /**
     * Creates a new aisle within a zone.
     * Aisle code must be unique within its zone.
     *
     * @param request Aisle details
     * @return The created AisleResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping( "/aisles" )
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "201", description = "Aisle successfully created within the zone")
    public ResponseEntity<AisleResponse> createAisle(@Valid @RequestBody AisleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createAisle(request));
    }

    /**
     * Returns all aisles within a zone.
     *
     * @param zoneId UUID of the zone
     * @return List of AisleResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping( "/{zoneId}/aisles" )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER', 'PICKER')")
    @ApiResponse(responseCode = "200", description = "List of aisles for the zone retrieved successfully")
    public ResponseEntity<List<AisleResponse>> getAislesByZone(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(locationService.getAislesByZone(zoneId));
    }

    /**
     * Creates a new shelf within an aisle.
     *
     * @param request Shelf details
     * @return The created ShelfResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping( "/shelves" )
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "201", description = "Shelf successfully created within the aisle")
    public ResponseEntity<ShelfResponse> createShelf(@Valid @RequestBody ShelfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createShelf(request));
    }

    /**
     * Returns all shelves within a zone across all its aisles.
     *
     * @param zoneId UUID of the zone
     * @return List of ShelfResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping( "/{zoneId}/shelves" )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER', 'RECEIVER', 'PICKER')")
    @ApiResponse(responseCode = "200", description = "List of shelves for the zone retrieved successfully")
    public ResponseEntity<List<ShelfResponse>> getShelvesByZone(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(locationService.getShelvesByZone(zoneId));
    }
}