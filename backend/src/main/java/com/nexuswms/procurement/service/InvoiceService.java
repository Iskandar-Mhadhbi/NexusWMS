package com.nexuswms.procurement.service;

import com.nexuswms.procurement.dto.request.InvoiceRequest;
import com.nexuswms.procurement.dto.response.InvoiceResponse;
import com.nexuswms.procurement.entity.*;
import com.nexuswms.procurement.repository.*;
import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final SupplierInvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptLineRepository receiptLineRepository;

    /* -------------------------------------------------------------------------
     * POST /invoices
     * Creates a supplier invoice against an existing purchase order.
     * Invoice number must be globally unique — suppliers use their own numbering.
     * supplierId is denormalized from PO to speed up invoice queries.
     * Status starts as PENDING, threeWayMatchStatus starts as PENDING.
     * ------------------------------------------------------------------------- */
    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        PurchaseOrder po = poRepository.findById(request.purchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + request.purchaseOrderId()));

        if (invoiceRepository.findByInvoiceNumber(request.invoiceNumber()).isPresent()) {
            throw new ConflictException("Invoice number already exists: " + request.invoiceNumber());
        }

        SupplierInvoice invoice = SupplierInvoice.builder()
                .purchaseOrder(po)
                .supplierId(po.getSupplier().getId())
                .invoiceNumber(request.invoiceNumber())
                .invoiceAmount(request.invoiceAmount())
                .build();

        return toResponse(invoiceRepository.save(invoice), po);
    }

    /* -------------------------------------------------------------------------
     * POST /invoices/{id}/match
     * Performs the 3-way match: PO total == received value == invoice amount.
     * Received value is calculated from all goods receipt lines for this PO,
     * priced using the unit prices from the original PO lines.
     * MATCHED → invoice APPROVED. Any discrepancy → invoice REJECTED.
     * This is the application-layer implementation; V7 migration will add
     * a stored procedure for the same logic at the DB level.
     * ------------------------------------------------------------------------- */
    @Transactional
    public InvoiceResponse match(UUID invoiceId) {
        SupplierInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        PurchaseOrder po = invoice.getPurchaseOrder();
        List<PurchaseOrderLine> poLines = poLineRepository.findByPurchaseOrder_Id(po.getId());

        BigDecimal receivedValue = receiptRepository.findByPurchaseOrder_Id(po.getId()).stream()
                .flatMap(r -> receiptLineRepository.findByGoodsReceipt_Id(r.getId()).stream())
                .map(line -> {
                    BigDecimal unitPrice = poLines.stream()
                            .filter(pol -> pol.getId().equals(line.getPoLineId()))
                            .findFirst()
                            .map(pol -> pol.getUnitPrice())
                            .orElse(BigDecimal.ZERO);
                    return unitPrice.multiply(BigDecimal.valueOf(line.getQuantityReceived()));
                })
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        boolean matched = po.getTotalAmount().compareTo(receivedValue) == 0
                && receivedValue.compareTo(invoice.getInvoiceAmount()) == 0;

        invoice.setThreeWayMatchStatus(matched ? ThreeWayMatchStatus.MATCHED : ThreeWayMatchStatus.DISCREPANCY);
        invoice.setStatus(matched ? InvoiceStatus.APPROVED : InvoiceStatus.REJECTED);

        return toResponse(invoiceRepository.save(invoice), po);
    }

    /* -------------------------------------------------------------------------
     * GET /invoices
     * Returns all invoices with PO and supplier details.
     * readOnly = true — keeps session open for lazy-loaded PO and supplier.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAll() {
        return invoiceRepository.findAll().stream()
                .map(i -> toResponse(i, i.getPurchaseOrder()))
                .toList();
    }

    /* -------------------------------------------------------------------------
     * GET /invoices/{id}
     * Returns a single invoice with PO and supplier details.
     * readOnly = true — keeps session open for lazy-loaded PO and supplier.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        SupplierInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        return toResponse(invoice, invoice.getPurchaseOrder());
    }

    /* -------------------------------------------------------------------------
     * Maps SupplierInvoice + PurchaseOrder to the response DTO.
     * Accesses po.getSupplier().getName() — requires an active transaction.
     * Never annotate private helpers with @Transactional — Spring AOP
     * does not proxy internal method calls, the annotation has no effect.
     * ------------------------------------------------------------------------- */
    private InvoiceResponse toResponse(SupplierInvoice i, PurchaseOrder po) {
        return new InvoiceResponse(
                i.getId(), po.getId(), po.getPoNumber(),
                i.getSupplierId(), po.getSupplier().getName(),
                i.getInvoiceNumber(), i.getInvoiceAmount(),
                i.getStatus(), i.getThreeWayMatchStatus(),
                i.getCreatedAt());
    }
}