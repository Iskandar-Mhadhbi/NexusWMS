package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.request.PackingCompleteRequest;
import com.nexuswms.fulfillment.dto.response.PackingTaskResponse;
import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.service.PackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse; 
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * REST controller for packing task management operations.
 *
 * <p>Packing tasks are created by managers from completed pick lists and
 * assigned to packers. Packers start and complete tasks, triggering parcel
 * creation with auto-generated tracking numbers and barcodes ready for dispatch.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/packing-tasks")
@RequiredArgsConstructor
@Tag(name = "Packing Tasks", description = "Packing task creation, assignment, and completion")
public class PackingController {

    private final PackingService packingService;

    /**
     * Creates a packing task from a completed pick list and assigns it to a packer.
     * An optional packing station can be specified for physical station assignment.
     * Pick list must be in COMPLETED status before a packing task can be created.
     *
     * @param pickListId      UUID of the completed pick list to create a task for.
     * @param assignedTo      UUID of the packer to assign the task to.
     * @param stationId       Optional UUID of the packing station to assign.
     * @param principalUserId UUID of the authenticated manager extracted from the JWT.
     * @return 201 Created with the created packing task.
     */
    @Operation(summary = "Create a packing task from a completed pick list")
    @ApiResponse(responseCode = "201", description = "Packing task created successfully") 
    @PostMapping( )
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PackingTaskResponse> createTask(
            @RequestParam UUID pickListId,
            @RequestParam UUID assignedTo,
            @RequestParam(required = false) UUID stationId,
            @AuthenticationPrincipal String principalUserId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packingService.createTask(
                        pickListId,
                        assignedTo,
                        stationId,
                        UUID.fromString(principalUserId)));
    }

    /**
     * Marks a packing task as IN_PROGRESS and records the start time.
     * Called by the packer when they begin physically packing the items.
     *
     * @param id UUID of the packing task to start.
     * @return 200 OK with the updated packing task reflecting IN_PROGRESS status.
     */
    @Operation(summary = "Start a packing task")
    @ApiResponse(responseCode = "200", description = "Packing task started successfully") 
    @PostMapping( "/{id}/start" )
    @PreAuthorize("hasAnyRole('ADMIN', 'PACKER')")
    public ResponseEntity<PackingTaskResponse> startTask(@PathVariable UUID id, @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(packingService.startTask(id,UUID.fromString(principalUserId)));
    }

    /**
     * Completes a packing task and creates a parcel with tracking number and barcode.
     * Weight and dimensions are required to generate the shipping label.
     * Transitions the order from PICKING to PACKING status.
     *
     * @param id              UUID of the packing task to complete.
     * @param request         Payload containing parcel weight and dimensions.
     * @param principalUserId UUID of the authenticated packer extracted from the JWT.
     * @return 201 Created with the created parcel including tracking number and barcode.
     */
    @Operation(summary = "Complete a packing task and create a parcel")
    @ApiResponse(responseCode = "201", description = "Packing task completed and parcel created") 
    @PostMapping( "/{id}/complete" )
    @PreAuthorize("hasAnyRole('ADMIN', 'PACKER')")
    public ResponseEntity<ParcelResponse> completeTask(
            @PathVariable UUID id,
            @Valid @RequestBody PackingCompleteRequest request,
            @AuthenticationPrincipal String principalUserId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packingService.completeTask(id, request, UUID.fromString(principalUserId)));
    }

    /**
     * Returns all packing tasks assigned to the currently authenticated packer.
     *
     * @param principalUserId UUID of the authenticated packer extracted from the JWT.
     * @return 200 OK with the list of packing tasks assigned to the current worker.
     */
    @Operation(summary = "Get packing tasks assigned to the current worker")
    @ApiResponse(responseCode = "200", description = "Packing tasks retrieved successfully") 
    @GetMapping( "/my" )
    @PreAuthorize("hasAnyRole('ADMIN', 'PACKER')")
    public ResponseEntity<List<PackingTaskResponse>> getMyTasks(
            @AuthenticationPrincipal String principalUserId
    ) {
        return ResponseEntity.ok(packingService.getByWorker(UUID.fromString(principalUserId)));
    }
}