// DispatchController.java
package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.DispatchRequest;
import com.nexuswms.fulfillment.dto.response.ShipmentResponse;
import com.nexuswms.fulfillment.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v{version}/parcels")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    /* ----- POST /parcels/{id}/dispatch ----- */
    /*
     * Dispatches a parcel by assigning a carrier and creating a shipment.
     * Transitions the parcel to DISPATCHED and the order to DISPATCHED.
     * Accessible by DISPATCHER role.
     */
    @PostMapping(value = "/{id}/dispatch", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ShipmentResponse> dispatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal String principalUserId,
            @Valid @RequestBody DispatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dispatchService.dispatch(id, UUID.fromString(principalUserId), request));
    }

    /* ----- GET /parcels/{id}/shipment ----- */
    /*
     * Returns the shipment associated with a given parcel.
     * Accessible by ADMIN, MANAGER, and DISPATCHER roles.
     */
    @GetMapping(value = "/{id}/shipment", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable UUID id) {
        return ResponseEntity.ok(dispatchService.getByParcel(id));
    }
}