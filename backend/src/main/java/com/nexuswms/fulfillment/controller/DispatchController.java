package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.DispatchRequest;
import com.nexuswms.fulfillment.dto.response.ShipmentResponse;
import com.nexuswms.fulfillment.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for parcel dispatch operations.
 *
 * <p>Handles carrier assignment and shipment creation for packed parcels.
 * Dispatching a parcel closes the fulfillment lifecycle — the parcel,
 * its order, and the fulfillment request all transition to DISPATCHED/COMPLETED.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/parcels")
@RequiredArgsConstructor
@Tag(name = "Parcels & Dispatch", description = "Parcel dispatch and shipment tracking")
public class DispatchController {

    private final DispatchService dispatchService;

    /**
     * Dispatches a parcel by assigning a carrier and creating a shipment record.
     * A carrier tracking number is auto-generated using the carrier's code.
     * Transitions the parcel to DISPATCHED, the order to DISPATCHED,
     * and the fulfillment request to COMPLETED.
     *
     * @param id              UUID of the parcel to dispatch.
     * @param principalUserId UUID of the authenticated dispatcher extracted from the JWT.
     * @param request         Payload containing the carrier ID and estimated delivery date.
     * @return 201 Created with the created shipment including carrier tracking number.
     */
    @Operation(summary = "Dispatch a parcel and create a shipment") 
    @ApiResponse(responseCode = "201", description = "Parcel dispatched and shipment created")
    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ShipmentResponse> dispatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal String principalUserId,
            @Valid @RequestBody DispatchRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dispatchService.dispatch(id, UUID.fromString(principalUserId), request));
    }

    /**
     * Returns the shipment record associated with a given parcel.
     *
     * @param id UUID of the parcel whose shipment to retrieve.
     * @return 200 OK with the shipment details including carrier and tracking number.
     */
    @Operation(summary = "Get shipment details for a parcel")
    @ApiResponse(responseCode = "200", description = "Shipment retrieved successfully") 
    @GetMapping("/{id}/shipment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable UUID id) {
        return ResponseEntity.ok(dispatchService.getByParcel(id));
    }
}