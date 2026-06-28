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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v{version}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /* ----- POST /orders ----- */
    /*
     * Creates a new order with its lines.
     * Accessible by ADMIN and MANAGER roles.
     * Order number is auto-generated — do not include it in the request.
     */
    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal String principalUserId,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request, UUID.fromString(principalUserId)));
    }

    /* ----- GET /orders ----- */
    /*
     * Returns all orders in the system.
     * Accessible by ADMIN and MANAGER roles.
     */
    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<OrderResponse>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    /* ----- GET /orders/{id} ----- */
    /*
     * Returns a single order by its UUID.
     * Accessible by ADMIN and MANAGER roles.
     */
    @GetMapping(value = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    /* ----- POST /orders/{id}/cancel ----- */
    /*
     * Cancels an order. Only RECEIVED or VALIDATED orders can be cancelled.
     * Accessible by ADMIN and MANAGER roles.
     */
    @PostMapping(value = "/{id}/cancel", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID id,@AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(orderService.cancel(id,UUID.fromString(principalUserId)));
    }

    /* ----- POST /orders/{id}/fulfillment-request ----- */
    /*
     * Validates an order and generates a fulfillment request for it.
     * Transitions the order from RECEIVED to VALIDATED.
     * Accessible by ADMIN and MANAGER roles.
     */
    @PostMapping(value = "/{id}/fulfillment-request", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<FulfillmentRequestResponse> generateFulfillmentRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.generateFulfillmentRequest(id, UUID.fromString(principalUserId)));
    }
}