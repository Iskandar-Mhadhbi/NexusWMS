package com.nexuswms.procurement.repository;

import com.nexuswms.procurement.entity.InvoiceStatus;
import com.nexuswms.procurement.entity.SupplierInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, UUID> {

    List<SupplierInvoice> findByPurchaseOrder_Id(UUID purchaseOrderId);

    List<SupplierInvoice> findBySupplierId(UUID supplierId);

    List<SupplierInvoice> findByStatus(InvoiceStatus status);

    Optional<SupplierInvoice> findByInvoiceNumber(String invoiceNumber);
}