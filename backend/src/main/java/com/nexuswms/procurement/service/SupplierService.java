package com.nexuswms.procurement.service;

import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.procurement.dto.request.SupplierRequest;
import com.nexuswms.procurement.dto.response.SupplierResponse;
import com.nexuswms.procurement.entity.Supplier;
import com.nexuswms.procurement.entity.SupplierStatus;
import com.nexuswms.procurement.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    /* -------------------------------------------------------------------------
     * POST /suppliers
     * Creates a new supplier. Code must be globally unique and uppercase
     * alphanumeric — enforced at DTO level and checked here before save.
     * Status always starts as ACTIVE — suppliers are created ready to use.
     * paymentTerms defaults to 30 (Net 30) if not provided by the client.
     * ------------------------------------------------------------------------- */
    public SupplierResponse create(SupplierRequest request) {
        if (supplierRepository.existsByCode(request.code())) {
            throw new ConflictException("Supplier with code " + request.code() + " already exists");
        }
        Supplier supplier = Supplier.builder()
                .name(request.name())
                .code(request.code())
                .contactInfo(request.contactInfo())
                .paymentTerms(request.paymentTerms() != null ? request.paymentTerms() : 30)
                .rating(request.rating())
                .status(SupplierStatus.ACTIVE)
                .build();
        return toResponse(supplierRepository.save(supplier));
    }

    /* -------------------------------------------------------------------------
     * GET /suppliers
     * Returns all suppliers. No @Transactional needed — Supplier has no
     * lazy-loaded relationships, no session required beyond the query.
     * ------------------------------------------------------------------------- */
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }
    /* -------------------------------------------------------------------------
     * GET /suppliers/{id}
     * Returns a single supplier by UUID. 404 if not found.
     * ------------------------------------------------------------------------- */
    public SupplierResponse getById(UUID id) {
        return toResponse(supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id)));
    }

    /* -------------------------------------------------------------------------
     * Maps Supplier entity to response DTO.
     * No relationships to resolve — all fields are direct columns.
     * ------------------------------------------------------------------------- */
    private SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(
                s.getId(), s.getName(), s.getCode(),
                s.getContactInfo(), s.getPaymentTerms(),
                s.getRating(), s.getStatus(), s.getCreatedAt());
    }
}