package com.nexuswms.fulfillment.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a single barcode scan event.
 * Append-only audit record — never updated after creation.
 * Schema is flexible per scan type — not all fields are populated for every scan.
 */
@Document(collection = "scan_logs")
public class ScanLog {

    @Id
    private String id;

    private String barcode;
    private String scanType;
    private String result;
    private String workerId;
    private String zone;
    private String packageId;
    private String skuId;
    private Instant timestamp;

    public ScanLog(String barcode, String scanType, String result,
                   String workerId, String zone, String packageId,
                   String skuId) {
        this.barcode = barcode;
        this.scanType = scanType;
        this.result = result;
        this.workerId = workerId;
        this.zone = zone;
        this.packageId = packageId;
        this.skuId = skuId;
        this.timestamp = Instant.now();
    }

    /* ----- Getters ------------------------------------------------------ */

    public String getId()        { return id; }
    public String getBarcode()   { return barcode; }
    public String getScanType()  { return scanType; }
    public String getResult()    { return result; }
    public String getWorkerId()  { return workerId; }
    public String getZone()      { return zone; }
    public String getPackageId() { return packageId; }
    public String getSkuId()     { return skuId; }
    public Instant getTimestamp(){ return timestamp; }
}