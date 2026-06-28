package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.fulfillment.document.ScanLog;
import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.entity.Parcel;
import com.nexuswms.fulfillment.repository.ParcelRepository;
import com.nexuswms.fulfillment.repository.ScanLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScanService {

    private final ParcelRepository parcelRepository;
    private final ScanLogRepository scanLogRepository;

    /* ----- Scan Barcode ----- */
    /**
     * Universal scan endpoint handler. Resolves a barcode to its parcel,
     * writes an audit record to MongoDB scan_logs, and returns the current
     * parcel state.
     *
     * @param barcode   the scanned barcode string
     * @param scannedBy the UUID of the worker performing the scan (from JWT principal)
     * @return the resolved parcel as a response DTO
     * @throws ResourceNotFoundException if no parcel matches the barcode
     */
    @Transactional(readOnly = true)
    public ParcelResponse scan(String barcode, UUID scannedBy) {
        Parcel parcel = parcelRepository.findAll().stream()
                .filter(p -> p.getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No parcel found with barcode: " + barcode));

        scanLogRepository.save(new ScanLog(
                barcode,
                "PARCEL_SCAN",
                "SUCCESS",
                scannedBy.toString(),
                "DISPATCH",
                parcel.getId().toString(),
                null
        ));

        return ParcelResponse.from(parcel);
    }
}