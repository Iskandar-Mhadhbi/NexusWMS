package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.document.OrderEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderEventRepository extends MongoRepository<OrderEvent, String> {

    List<OrderEvent> findByOrderIdOrderByTimestampAsc(String orderId);
}