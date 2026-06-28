// ScanController.java
package com.nexuswms.fulfillment.controller;

import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v{version}/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    /* ----- POST /scan ----- */
    /*
     * Universal barcode scan endpoint.
     * Resolves a barcode to its parcel and returns current parcel state.
     * In Phase 5 this will additionally write to MongoDB scan_logs
     * and publish a WebSocket event to the manager dashboard.
     * Accessible by all authenticated warehouse roles.
     */
    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEIVER', 'PICKER', 'PACKER', 'DISPATCHER')")
    public ResponseEntity<ParcelResponse> scan(
            @RequestParam String barcode,
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(scanService.scan(barcode, UUID.fromString(principalUserId)));
    }
}