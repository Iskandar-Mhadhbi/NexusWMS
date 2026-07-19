// OrderController.java
package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.OrderRequest;
import com.nexuswms.fulfillment.dto.response.FulfillmentRequestResponse;
import com.nexuswms.fulfillment.dto.response.OrderResponse;
import com.nexuswms.fulfillment.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse; 
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for order management operations.
 *
 * <p>Handles the intake and lifecycle of customer orders from creation
 * through to fulfillment request generation. All endpoints are restricted
 * to ADMIN and MANAGER roles — operational staff interact with orders
 * indirectly via pick lists and packing tasks.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order creation, retrieval, cancellation, and fulfillment request generation")
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a new order with its associated order lines.
     * The order number is auto-generated — do not include it in the request.
     * Initial status is RECEIVED.
     *
     * @param principalUserId UUID of the authenticated manager extracted from the JWT.
     * @param request         Order payload containing customer details, priority, and lines.
     * @return 201 Created with the created order and its lines.
     */
    @Operation(summary = "Create a new order")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal String principalUserId,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request, UUID.fromString(principalUserId)));
    }

    /**
     * Returns all orders in the system ordered by creation date descending.
     *
     * @return 200 OK with the full list of orders.
     */
    @Operation(summary = "List all orders")
    @ApiResponse(responseCode = "200", description = "Order list retrieved successfully")
    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<OrderResponse>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    /**
     * Returns a single order by its UUID including all order lines.
     *
     * @param id The UUID of the order to retrieve.
     * @return 200 OK with the order details.
     */
    @Operation(summary = "Get order by ID")
    @ApiResponse(responseCode = "200", description = "Order retrieved successfully")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    /**
     * Cancels an order. Only orders in RECEIVED or VALIDATED status can be cancelled.
     * Orders already in PICKING or beyond cannot be cancelled.
     *
     * @param id              The UUID of the order to cancel.
     * @param principalUserId UUID of the authenticated manager extracted from the JWT.
     * @return 200 OK with the updated order reflecting CANCELLED status.
     */
    @Operation(summary = "Cancel an order")
    @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal String principalUserId
    ) {
        return ResponseEntity.ok(orderService.cancel(id, UUID.fromString(principalUserId)));
    }

    /**
     * Validates an order and generates a fulfillment request for warehouse processing.
     * Transitions the order from RECEIVED to VALIDATED status.
     * A fulfillment request is the trigger for pick list generation.
     *
     * @param id              The UUID of the order to validate.
     * @param principalUserId UUID of the authenticated manager extracted from the JWT.
     * @return 201 Created with the generated fulfillment request.
     */
    @Operation(summary = "Generate a fulfillment request for an order")
    @ApiResponse(responseCode = "201", description = "Fulfillment request created successfully")
    @PostMapping("/{id}/fulfillment-request")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<FulfillmentRequestResponse> generateFulfillmentRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal String principalUserId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.generateFulfillmentRequest(id, UUID.fromString(principalUserId)));
    }
}