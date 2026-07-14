package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.ReorderAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReorderAlertRepository extends JpaRepository<ReorderAlert, UUID> {
    List<ReorderAlert> findByStatus(String status);
    List<ReorderAlert> findBySkuId(UUID skuId);
    Optional<ReorderAlert> findBySkuIdAndStatus(UUID skuId, String status);
    boolean existsBySkuIdAndStatus(UUID skuId, String status);
}