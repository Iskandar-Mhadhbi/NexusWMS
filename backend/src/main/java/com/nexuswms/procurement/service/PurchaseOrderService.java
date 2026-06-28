package com.nexuswms.procurement.service;

import com.nexuswms.procurement.dto.request.PurchaseOrderRequest;
import com.nexuswms.procurement.dto.response.PurchaseOrderLineResponse;
import com.nexuswms.procurement.dto.response.PurchaseOrderResponse;
import com.nexuswms.procurement.entity.*;
import com.nexuswms.procurement.repository.*;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.inventory.entity.Sku;
import com.nexuswms.inventory.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final SupplierRepository supplierRepository;
    private final SkuService skuService;

    /* -------------------------------------------------------------------------
     * POST /purchase-orders
     * Creates a new purchase order in DRAFT status.
     * Validates supplier is ACTIVE and all SKUs exist before saving.
     * Batch-fetches SKUs to avoid N+1. Total amount is calculated server-side
     * from line quantities and unit prices — never trusted from the client.
     * PO number is auto-generated in format PO-YYYYMMDD-XXXXX.
     * ------------------------------------------------------------------------- */
    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request, UUID requestedBy) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot create PO for a supplier that is not ACTIVE");
        }

        Set<UUID> skuIds = request.lines().stream()
                .map(l -> l.skuId())
                .collect(Collectors.toSet());
        Map<UUID, Sku> skuMap = skuService.getSkuMapByIds(skuIds);
        for (UUID skuId : skuIds) {
            if (!skuMap.containsKey(skuId)) {
                throw new ResourceNotFoundException("SKU not found: " + skuId);
            }
        }

        BigDecimal total = request.lines().stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantityOrdered())))
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .supplier(supplier)
                .requestedBy(requestedBy)
                .expectedDelivery(request.expectedDelivery())
                .totalAmount(total)
                .build();
        po = poRepository.save(po);

        final PurchaseOrder savedPo = po;
        List<PurchaseOrderLine> lines = request.lines().stream()
                .map(l -> PurchaseOrderLine.builder()
                        .purchaseOrder(savedPo)
                        .skuId(l.skuId())
                        .quantityOrdered(l.quantityOrdered())
                        .unitPrice(l.unitPrice())
                        .build())
                .toList();
        poLineRepository.saveAll(lines);

        return toResponse(savedPo, lines, supplier, skuMap);
    }

    /* -------------------------------------------------------------------------
     * POST /purchase-orders/{id}/approve
     * Transitions a PO from DRAFT or PENDING_APPROVAL to APPROVED.
     * Only ADMIN and MANAGER roles can approve (enforced at controller level).
     * Records who approved and when via approvedBy field.
     * ------------------------------------------------------------------------- */
    @Transactional
    public PurchaseOrderResponse approve(UUID poId, UUID approvedBy) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + poId));
        if (po.getStatus() != PurchaseOrderStatus.DRAFT
                && po.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Only DRAFT or PENDING_APPROVAL orders can be approved");
        }
        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedBy(approvedBy);
        po = poRepository.save(po);
        return enrichAndConvertToResponse(po);
    }

    /* -------------------------------------------------------------------------
     * GET /purchase-orders
     * Returns all purchase orders with their lines and SKU details.
     * readOnly = true — no writes, allows Hibernate to skip dirty checking.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAll() {
        return poRepository.findAll().stream()
                .map(this::enrichAndConvertToResponse)
                .toList();
    }

    /* -------------------------------------------------------------------------
     * GET /purchase-orders/{id}
     * Returns a single purchase order with its lines and SKU details.
     * readOnly = true — no writes, allows Hibernate to skip dirty checking.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(UUID id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
        return enrichAndConvertToResponse(po);
    }

    /* -------------------------------------------------------------------------
     * Shared helper used by approve(), getAll(), getById().
     * Fetches PO lines and enriches them with SKU details via batch lookup.
     * Requires an active transaction — supplier and lines are lazy-loaded here.
     * ------------------------------------------------------------------------- */
    private PurchaseOrderResponse enrichAndConvertToResponse(PurchaseOrder po) {
        List<PurchaseOrderLine> lines = poLineRepository.findByPurchaseOrder_Id(po.getId());
        Set<UUID> skuIds = lines.stream()
                .map(line -> Objects.requireNonNull(line.getSkuId(), "Purchase order line is missing a SKU ID"))
                .collect(Collectors.toSet());
        Map<UUID, Sku> skuMap = skuService.getSkuMapByIds(skuIds);
        return toResponse(po, lines, po.getSupplier(), skuMap);
    }

    /* -------------------------------------------------------------------------
     * Generates a unique PO number in format PO-YYYYMMDD-XXXXX.
     * Retries on collision — statistically negligible but safe.
     * ------------------------------------------------------------------------- */
    private String generatePoNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String poNumber;
        do {
            String random = String.format("%05d", (int) (Math.random() * 100000));
            poNumber = "PO-" + date + "-" + random;
        } while (poRepository.existsByPoNumber(poNumber));
        return poNumber;
    }

    /* -------------------------------------------------------------------------
     * Maps PurchaseOrder + lines + SKU data to the response DTO.
     * lineTotal is computed here rather than stored — derived from unit price
     * and quantity, no need to persist it separately.
     * ------------------------------------------------------------------------- */
    private PurchaseOrderResponse toResponse(PurchaseOrder po, List<PurchaseOrderLine> lines,
                                              Supplier supplier, Map<UUID, Sku> skuMap) {
        List<PurchaseOrderLineResponse> lineResponses = lines.stream()
                .map(l -> {
                    Sku sku = skuMap.get(l.getSkuId());
                    return new PurchaseOrderLineResponse(
                            l.getId(), l.getSkuId(),
                            sku != null ? sku.getSkuCode() : null,
                            sku != null ? sku.getName() : null,
                            l.getQuantityOrdered(), l.getQuantityReceived(),
                            l.getUnitPrice(),
                            l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantityOrdered())),
                            l.getStatus());
                }).toList();

        return new PurchaseOrderResponse(
                po.getId(), po.getPoNumber(),
                supplier.getId(), supplier.getName(),
                po.getRequestedBy(), po.getApprovedBy(),
                po.getStatus(), po.getExpectedDelivery(),
                po.getTotalAmount(), po.getCreatedAt(),
                lineResponses);
    }
}