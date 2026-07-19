package com.nexuswms.procurement.repository;

import com.nexuswms.procurement.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

    List<GoodsReceipt> findByPurchaseOrder_Id(UUID purchaseOrderId);
    boolean existsByGrNumber(String grNumber);
}