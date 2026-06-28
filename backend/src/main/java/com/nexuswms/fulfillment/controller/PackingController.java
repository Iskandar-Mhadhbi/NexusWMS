// PackingController.java
package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.PackingCompleteRequest;
import com.nexuswms.fulfillment.dto.response.PackingTaskResponse;
import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.service.PackingService;
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
@RequestMapping("/api/v{version}/packing-tasks")
@RequiredArgsConstructor
public class PackingController {

    private final PackingService packingService;

    /* ----- POST /packing-tasks ----- */
    /*
     * Creates a packing task from a completed pick list.
     * pickListId and assignedTo are required params. stationId is optional.
     * Accessible by ADMIN and MANAGER roles.
     */
    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PackingTaskResponse> createTask(
            @RequestParam UUID pickListId,
            @RequestParam UUID assignedTo,
            @RequestParam(required = false) UUID stationId,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packingService.createTask(
                        pickListId,
                        assignedTo,
                        stationId,
                        UUID.fromString(principalUserId)));
    }

    /* ----- POST /packing-tasks/{id}/start ----- */
    /*
     * Marks a packing task as IN_PROGRESS.
     * Called by the packer when they begin working on the task.
     * Accessible by PACKER role.
     */
    @PostMapping(value = "/{id}/start", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN','PACKER')")
    public ResponseEntity<PackingTaskResponse> startTask(@PathVariable UUID id) {
        return ResponseEntity.ok(packingService.startTask(id));
    }

    /* ----- POST /packing-tasks/{id}/complete ----- */
    /*
     * Completes a packing task and creates a parcel with tracking number and barcode.
     * Weight and dimensions are required in the request body.
     * Accessible by PACKER role.
     */
    @PostMapping(value = "/{id}/complete", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN','PACKER')")
    public ResponseEntity<ParcelResponse> completeTask(
            @PathVariable UUID id,
            @Valid @RequestBody PackingCompleteRequest request,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packingService.completeTask(id, request,UUID.fromString(principalUserId)));
    }

    /* ----- GET /packing-tasks/my ----- */
    /*
     * Returns all packing tasks assigned to the currently authenticated packer.
     * Accessible by PACKER role.
     */
    @GetMapping(value = "/my", version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN','PACKER')")
    public ResponseEntity<List<PackingTaskResponse>> getMyTasks(
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(packingService.getByWorker(UUID.fromString(principalUserId)));
    }
}