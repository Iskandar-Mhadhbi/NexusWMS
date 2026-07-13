package com.nexuswms.procurement.controller;

import com.nexuswms.procurement.dto.request.PurchaseOrderRequest;
import com.nexuswms.procurement.dto.response.PurchaseOrderResponse;
import com.nexuswms.procurement.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing purchase orders in the procurement process.
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * Creates a new purchase order.
     *
     * @param request Contains the purchase order details.
     * @param principalUserId The ID of the authenticated user creating the PO (from JWT).
     * @return The created PurchaseOrderResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_CONTROLLER')")
    @ApiResponse(responseCode = "201", description = "Purchase order created successfully")
    public ResponseEntity<PurchaseOrderResponse> create(
            @Valid @RequestBody PurchaseOrderRequest request,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseOrderService.create(request, UUID.fromString(principalUserId)));
    }

    /**
     * Approves a pending purchase order.
     *
     * @param id The UUID of the purchase order to approve.
     * @param principalUserId The ID of the authenticated user approving the PO.
     * @return The updated PurchaseOrderResponse wrapped in a 200 OK ResponseEntity.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @ApiResponse(responseCode = "200", description = "Purchase order approved successfully")
    public ResponseEntity<PurchaseOrderResponse> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(purchaseOrderService.approve(id, UUID.fromString(principalUserId)));
    }

    /**
     * Retrieves all purchase orders in the system.
     *
     * @return A list of PurchaseOrderResponse objects wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @ApiResponse(responseCode = "200", description = "List of all purchase orders retrieved successfully")
    public ResponseEntity<List<PurchaseOrderResponse>> getAll() {
        return ResponseEntity.ok(purchaseOrderService.getAll());
    }

    /**
     * Retrieves a specific purchase order by its unique identifier.
     *
     * @param id The UUID of the purchase order to retrieve.
     * @return The PurchaseOrderResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ApiResponse(responseCode = "200", description = "Purchase order retrieved successfully by ID")
    public ResponseEntity<PurchaseOrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.getById(id));
    }
}