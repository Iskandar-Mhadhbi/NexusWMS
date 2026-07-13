package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse; 
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the universal barcode scan endpoint.
 *
 * <p>Simulates physical barcode scanner input at any warehouse station.
 * Resolves a barcode to its parcel and returns the current parcel state.
 * Every scan is logged to MongoDB for full audit traceability, and a
 * WebSocket event is published to the manager dashboard in real time.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/scan")
@RequiredArgsConstructor
@Tag(name = "Scan", description = "Universal barcode scan endpoint for all warehouse stations")
public class ScanController {

    private final ScanService scanService;

    /**
     * Processes a barcode scan from any warehouse station.
     * Resolves the barcode to its parcel, writes a scan log to MongoDB,
     * and publishes a WebSocket event to the manager dashboard.
     * Accessible by all authenticated warehouse roles.
     *
     * @param barcode         The scanned barcode string.
     * @param principalUserId UUID of the authenticated worker extracted from the JWT.
     * @return 200 OK with the current state of the scanned parcel.
     */
    @Operation(summary = "Process a barcode scan")
    @ApiResponse(responseCode = "200", description = "Barcode resolved successfully") 
    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEIVER', 'PICKER', 'PACKER', 'DISPATCHER')")
    public ResponseEntity<ParcelResponse> scan(
            @RequestParam String barcode,
            @AuthenticationPrincipal String principalUserId
    ) {
        return ResponseEntity.ok(scanService.scan(barcode, UUID.fromString(principalUserId)));
    }
}