package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.response.ShipmentResponse;
import com.nexuswms.fulfillment.entity.ShipmentStatus; // adjust import to wherever this enum actually lives
import com.nexuswms.fulfillment.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for shipment tracking and history.
 *
 * <p>Shipments are created as a side effect of dispatching a parcel (see
 * {@link DispatchController#dispatch}), but are treated as an independent,
 * queryable resource here — mirroring how PickList and PackingTask each
 * get their own controller despite being generated from an Order.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Shipment tracking and history")
public class ShipmentController {

    private final DispatchService dispatchService;

    /**
     * Returns all shipments, optionally filtered by status.
     * Manager-facing oversight query.
     *
     * @param status optional status filter; if null, returns all shipments.
     * @return 200 OK with all matching shipments.
     */
    @Operation(summary = "Get all shipments (manager oversight)")
    @ApiResponse(responseCode = "200", description = "Shipments retrieved successfully")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ShipmentResponse>> getAll(
            @RequestParam(required = false) ShipmentStatus status
    ) {
        return ResponseEntity.ok(dispatchService.getAll(status));
    }

    /**
     * Returns a single shipment by its own UUID.
     *
     * @param id UUID of the shipment to retrieve.
     * @return 200 OK with the shipment details.
     */
    @Operation(summary = "Get shipment by ID")
    @ApiResponse(responseCode = "200", description = "Shipment retrieved successfully")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<ShipmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(dispatchService.getById(id));
    }
}