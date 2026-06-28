// CarrierController.java
package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.CarrierRequest;
import com.nexuswms.fulfillment.dto.response.CarrierResponse;
import com.nexuswms.fulfillment.service.CarrierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v{version}/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierService carrierService;

    /* ----- POST /carriers ----- */
    /*
     * Creates a new carrier. Carrier code must be unique.
     * Accessible by ADMIN role only.
     */
    @PostMapping(version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CarrierResponse> create(
            @Valid @RequestBody CarrierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carrierService.create(request));
    }

    /* ----- GET /carriers ----- */
    /*
     * Returns all carriers regardless of active status.
     * Accessible by ADMIN and MANAGER roles.
     */
    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<CarrierResponse>> getAll() {
        return ResponseEntity.ok(carrierService.getAll());
    }

    /* ----- GET /carriers/active ----- */
    /*
     * Returns only active carriers. Used by dispatchers during assignment.
     * Accessible by ADMIN, MANAGER, and DISPATCHER roles.
     */
    @GetMapping(value = "/active", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<CarrierResponse>> getAllActive() {
        return ResponseEntity.ok(carrierService.getAllActive());
    }

    /* ----- GET /carriers/{id} ----- */
    /*
     * Returns a single carrier by its UUID.
     * Accessible by ADMIN and MANAGER roles.
     */
    @GetMapping(value = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CarrierResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(carrierService.getById(id));
    }
}