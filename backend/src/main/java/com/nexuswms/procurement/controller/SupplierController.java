package com.nexuswms.procurement.controller;

import com.nexuswms.procurement.dto.request.SupplierRequest;
import com.nexuswms.procurement.dto.response.SupplierResponse;
import com.nexuswms.procurement.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v{version}/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * Creates a new supplier record.
     *
     * @param request Contains the supplier details (name, code, contact info, payment terms, rating).
     * @return The created SupplierResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }
    /**
     * Retrieves all suppliers in the system.
     *
     * @return A list of SupplierResponse objects wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping(version = "1.0")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SupplierResponse>> getAll() {
        return ResponseEntity.ok(supplierService.getAll());
    }
    /**
     * Retrieves a specific supplier by its unique identifier.
     *
     * @param id The UUID of the supplier to retrieve.
     * @return The SupplierResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping(value = "/{id}", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SupplierResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }
}