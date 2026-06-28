package com.nexuswms.procurement.controller;

import com.nexuswms.procurement.dto.request.InvoiceRequest;
import com.nexuswms.procurement.dto.response.InvoiceResponse;
import com.nexuswms.procurement.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing financial invoices within the procurement process.
 * Handles creation, retrieval, and the matching process of invoices against purchase orders and receipts.
 */
@RestController
@RequestMapping("/api/v{version}/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Creates a new invoice record in the system.
     * * @param request The payload containing invoice details (e.g., supplier, amounts, reference numbers).
     * @return The created InvoiceResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    /**
     * Triggers the matching process for a specific invoice.
     * Often referred to as "3-way matching" (Invoice vs. Purchase Order vs. Goods Receipt).
     * * @param id The UUID of the invoice to match.
     * @return The updated InvoiceResponse showing the match status, wrapped in a 200 OK ResponseEntity.
     */
    @PostMapping(value="/{id}/match", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<InvoiceResponse> match(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.match(id));
    }

    /**
     * Retrieves a list of all invoices in the system.
     * * @return A list of InvoiceResponse objects wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<List<InvoiceResponse>> getAll() {
        return ResponseEntity.ok(invoiceService.getAll());
    }

    /**
     * Retrieves a specific invoice by its unique identifier.
     * * @param id The UUID of the invoice to retrieve.
     * @return The InvoiceResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping(value="/{id}",version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }
}