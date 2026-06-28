package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.SkuLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuLocationRepository extends JpaRepository<SkuLocation, UUID> {
    List<SkuLocation> findBySkuId(UUID skuId);
    List<SkuLocation> findByShelfId(UUID shelfId);
    Optional<SkuLocation> findBySkuIdAndShelfIdAndBatchId(UUID skuId, UUID shelfId, String batchId);
    

    @Query("""
        SELECT COALESCE(SUM(sl.quantity), 0)
        FROM SkuLocation sl
        WHERE sl.sku.id = :skuId
    """)
    Integer getTotalQuantityBySkuId(UUID skuId);

    @Query("""
        SELECT COALESCE(SUM(sl.quantity - sl.reservedQuantity), 0)
        FROM SkuLocation sl
        WHERE sl.sku.id = :skuId
    """)
    Integer getAvailableQuantityBySkuId(UUID skuId);
    
}