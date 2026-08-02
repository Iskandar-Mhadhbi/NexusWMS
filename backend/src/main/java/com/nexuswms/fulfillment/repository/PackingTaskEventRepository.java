package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.document.PackingTaskEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PackingTaskEventRepository extends MongoRepository<PackingTaskEvent, String> {
    List<PackingTaskEvent> findByTaskIdOrderByTimestampAsc(String taskId);
}