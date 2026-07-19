package com.nexuswms.procurement.repository;

import com.nexuswms.procurement.entity.PurchaseOrder;
import com.nexuswms.procurement.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    boolean existsByPoNumber(String poNumber);

    List<PurchaseOrder> findBySupplier_Id(UUID supplierId);

    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    List<PurchaseOrder> findByRequestedBy(UUID userId);
}