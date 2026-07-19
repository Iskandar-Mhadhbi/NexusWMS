package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.CarrierRequest;
import com.nexuswms.fulfillment.dto.response.CarrierResponse;
import com.nexuswms.fulfillment.service.CarrierService;
import io.swagger.v3.oas.annotations.Operation;
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
 * REST controller for carrier management operations.
 *
 * <p>Carriers represent shipping companies (e.g. DHL, FedEx) used to dispatch
 * parcels. Active carriers are available for selection during dispatch.
 * Carrier creation is restricted to ADMIN — carrier data changes infrequently
 * and requires deliberate administrative action.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/carriers")
@RequiredArgsConstructor
@Tag(name = "Carriers", description = "Carrier management and retrieval")
public class CarrierController {

    private final CarrierService carrierService;

    /**
     * Creates a new carrier. Carrier code must be unique across the system.
     * The code is used as a prefix for auto-generated carrier tracking numbers
     * (e.g. DHL-7873669246).
     *
     * @param request Payload containing carrier name, code, and contact info.
     * @return 201 Created with the created carrier.
     */
    @Operation(summary = "Create a new carrier")
    @ApiResponse(responseCode = "201", description = "Carrier created successfully")
    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CarrierResponse> create(
            @Valid @RequestBody CarrierRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carrierService.create(request));
    }

    /**
     * Returns all carriers regardless of active status.
     * Used by admins and managers for carrier overview and management.
     *
     * @return 200 OK with the full list of carriers.
     */
    @Operation(summary = "List all carriers")
    @ApiResponse(responseCode = "200", description = "Carrier list retrieved successfully")
    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<CarrierResponse>> getAll() {
        return ResponseEntity.ok(carrierService.getAll());
    }

    /**
     * Returns only active carriers available for dispatch assignment.
     * Used by dispatchers when selecting a carrier for a parcel.
     *
     * @return 200 OK with the list of active carriers.
     */
    @Operation(summary = "List active carriers available for dispatch")
    @ApiResponse(responseCode = "200", description = "Active carrier list retrieved successfully")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CarrierResponse>> getAllActive() {
        return ResponseEntity.ok(carrierService.getAllActive());
    }

    /**
     * Returns a single carrier by its UUID.
     *
     * @param id UUID of the carrier to retrieve.
     * @return 200 OK with the carrier details.
     */
    @Operation(summary = "Get carrier by ID")
    @ApiResponse(responseCode = "200", description = "Carrier retrieved successfully")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CarrierResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(carrierService.getById(id));
    }
}