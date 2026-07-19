package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findBySkuIdOrderByMovedAtDesc(UUID skuId);
    List<StockMovement> findByWorkerIdOrderByMovedAtDesc(UUID workerId);
}