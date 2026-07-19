package com.nexuswms.procurement.controller;

import com.nexuswms.procurement.dto.request.GoodsReceiptRequest;
import com.nexuswms.procurement.dto.response.GoodsReceiptResponse;
import com.nexuswms.procurement.service.GoodsReceiptService;
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
 * REST Controller for handling goods receipt operations.
 * Records physical arrival of goods against purchase orders.
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    /**
     * Records the physical arrival of goods against an approved purchase order.
     * The receiving user ID is extracted from the authenticated JWT principal.
     *
     * @param request The goods receipt details provided by the client.
     * @param principalUserId The authenticated user ID performing the action at the dock.
     * @return The created GoodsReceiptResponse wrapped in a 201 Created ResponseEntity.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEIVER')")
    @ApiResponse(responseCode = "201", description = "Goods receipt recorded successfully")
    public ResponseEntity<GoodsReceiptResponse> create(
            @Valid @RequestBody GoodsReceiptRequest request,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goodsReceiptService.create(request, UUID.fromString(principalUserId)));
    }

    /**
     * Retrieves a single goods receipt and its associated line items.
     *
     * @param id The unique identifier (UUID) of the goods receipt.
     * @return The found GoodsReceiptResponse wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ApiResponse(responseCode = "200", description = "Goods receipt retrieved successfully by ID")
    public ResponseEntity<GoodsReceiptResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(goodsReceiptService.getById(id));
    }

    /**
     * Retrieves all goods receipts associated with a specific purchase order.
     * Supports scenarios where a single PO is fulfilled via multiple partial deliveries.
     *
     * @param poId The unique identifier (UUID) of the purchase order.
     * @return A list of GoodsReceiptResponse objects wrapped in a 200 OK ResponseEntity.
     */
    @GetMapping("/by-po/{poId}")
    @PreAuthorize("isAuthenticated()")
    @ApiResponse(responseCode = "200", description = "Goods receipts for the purchase order retrieved successfully")
    public ResponseEntity<List<GoodsReceiptResponse>> getByPurchaseOrder(@PathVariable UUID poId) {
        return ResponseEntity.ok(goodsReceiptService.getByPurchaseOrder(poId));
    }
}