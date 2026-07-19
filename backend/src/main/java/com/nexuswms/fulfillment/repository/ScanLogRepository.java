package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.document.ScanLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ScanLogRepository extends MongoRepository<ScanLog, String> {
}