// PickListController.java
package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.PickItemRequest;
import com.nexuswms.fulfillment.dto.response.PickListItemResponse;
import com.nexuswms.fulfillment.dto.response.PickListResponse;
import com.nexuswms.fulfillment.service.PickListService;
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
@RequestMapping("/api/v{version}/pick-lists")
@RequiredArgsConstructor
public class PickListController {

    private final PickListService pickListService;

    /* ----- POST /pick-lists/generate ----- */
    /*
     * Generates a pick list for a fulfillment request and assigns it to a picker.
     * Fulfillment request ID and assigned worker ID are passed as request params.
     * Accessible by ADMIN and MANAGER roles.
     */
    @PostMapping(value = "/generate", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PickListResponse> generate(
            @RequestParam UUID fulfillmentRequestId,
            @RequestParam UUID assignedTo,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pickListService.generate(
                        fulfillmentRequestId,
                        assignedTo,
                        UUID.fromString(principalUserId)));
    }

    /* ----- GET /pick-lists/my ----- */
    /*
     * Returns all pick lists assigned to the currently authenticated worker.
     * Accessible by PICKER role.
     */
    @GetMapping(value = "/my", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN','PICKER')")
    public ResponseEntity<List<PickListResponse>> getMyPickLists(
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(pickListService.getByWorker(UUID.fromString(principalUserId)));
    }

    /* ----- GET /pick-lists/{id} ----- */
    /*
     * Returns a single pick list by its UUID.
     * Accessible by ADMIN, MANAGER, and PICKER roles.
     */
    @GetMapping(value = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PICKER')")
    public ResponseEntity<PickListResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(pickListService.getById(id));
    }

    /* ----- POST /pick-lists/{id}/items/{itemId}/pick ----- */
    /*
     * Records the picking of a specific item on a pick list.
     * Called by the picker when they physically pick an item from the shelf.
     * Accessible by PICKER role.
     */
    @PostMapping(value = "/{id}/items/{itemId}/pick", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN','PICKER')")
    public ResponseEntity<PickListItemResponse> pickItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody PickItemRequest request,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(pickListService.pickItem(id, itemId, request,UUID.fromString(principalUserId)));
    }
}