package com.nexuswms.procurement.service;

import com.nexuswms.procurement.dto.request.PurchaseOrderRequest;
import com.nexuswms.procurement.dto.response.PurchaseOrderLineResponse;
import com.nexuswms.procurement.dto.response.PurchaseOrderResponse;
import com.nexuswms.procurement.entity.*;
import com.nexuswms.procurement.repository.*;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.inventory.entity.Sku;
import com.nexuswms.inventory.service.SkuService;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for purchase order lifecycle: creation and approval.
 *
 * <p>requestedBy and approvedBy identify the staff who requested and
 * approved a PO respectively — enriched to UserSummaryResponse for the
 * same accountability reasons as PickList/PackingTask/Shipment/GoodsReceipt.
 * approvedBy is nullable (a DRAFT PO has no approver yet), so enrichment
 * must handle its absence gracefully.</p>
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    /** Maximum attempts to generate a unique PO number before giving up. */
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final SupplierRepository supplierRepository;
    private final SkuService skuService;
    private final UserService userService;

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

        Map<UUID, UserSummaryResponse> userMap = userService.getUserSummaries(Set.of(requestedBy));

        return toResponse(savedPo, lines, supplier, skuMap, userMap);
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
     * Returns all purchase orders with their lines, SKU details, and
     * requester/approver user summaries.
     * Batch-fetches all user summaries in one call to avoid N+1 across POs.
     * readOnly = true — no writes, allows Hibernate to skip dirty checking.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getAll() {
        List<PurchaseOrder> orders = poRepository.findAll();
        if (orders.isEmpty()) return List.of();

        Set<UUID> userIds = orders.stream()
                .flatMap(po -> Stream.of(po.getRequestedBy(), po.getApprovedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryResponse> userMap = userService.getUserSummaries(userIds);

        return orders.stream()
                .map(po -> enrichAndConvertToResponse(po, userMap))
                .toList();
    }

    /* -------------------------------------------------------------------------
     * GET /purchase-orders/{id}
     * Returns a single purchase order with its lines, SKU details, and
     * requester/approver user summaries.
     * readOnly = true — no writes, allows Hibernate to skip dirty checking.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(UUID id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
        return enrichAndConvertToResponse(po);
    }

    /* -------------------------------------------------------------------------
     * Shared helper used by approve() and getById() — single-PO case.
     * Resolves requestedBy/approvedBy user summaries in one batched call,
     * then delegates to the list-aware overload.
     * Requires an active transaction — supplier and lines are lazy-loaded here.
     * ------------------------------------------------------------------------- */
    private PurchaseOrderResponse enrichAndConvertToResponse(PurchaseOrder po) {
        Set<UUID> userIds = Stream.of(po.getRequestedBy(), po.getApprovedBy())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryResponse> userMap = userService.getUserSummaries(userIds);
        return enrichAndConvertToResponse(po, userMap);
    }

    /* -------------------------------------------------------------------------
     * Shared helper used by getAll() — accepts a pre-batched user map so
     * callers iterating multiple POs don't re-query per PO.
     * Fetches PO lines and enriches them with SKU details via batch lookup.
     * ------------------------------------------------------------------------- */
    private PurchaseOrderResponse enrichAndConvertToResponse(PurchaseOrder po, Map<UUID, UserSummaryResponse> userMap) {
        List<PurchaseOrderLine> lines = poLineRepository.findByPurchaseOrder_Id(po.getId());
        Set<UUID> skuIds = lines.stream()
                .map(line -> Objects.requireNonNull(line.getSkuId(), "Purchase order line is missing a SKU ID"))
                .collect(Collectors.toSet());
        Map<UUID, Sku> skuMap = skuService.getSkuMapByIds(skuIds);
        return toResponse(po, lines, po.getSupplier(), skuMap, userMap);
    }

    /* -------------------------------------------------------------------------
     * Generates a unique PO number in format PO-YYYYMMDD-XXXXX.
     * Bounded retry on collision — throws after MAX_GENERATION_ATTEMPTS
     * rather than looping indefinitely, consistent with Order/PackingTask/
     * Shipment/GoodsReceipt number generation.
     * ------------------------------------------------------------------------- */
    private String generatePoNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "PO-" + datePart + "-" + String.format("%05d", new Random().nextInt(100000));
            if (!poRepository.existsByPoNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique PO number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    /* -------------------------------------------------------------------------
     * Maps PurchaseOrder + lines + SKU data + user data to the response DTO.
     * lineTotal is computed here rather than stored — derived from unit price
     * and quantity, no need to persist it separately.
     * approvedBy may be null (unapproved PO) — userMap.get() on a null key
     * safely returns null, no separate branch needed.
     * ------------------------------------------------------------------------- */
    private PurchaseOrderResponse toResponse(PurchaseOrder po, List<PurchaseOrderLine> lines,
                                              Supplier supplier, Map<UUID, Sku> skuMap,
                                              Map<UUID, UserSummaryResponse> userMap) {
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
                userMap.get(po.getRequestedBy()),
                userMap.get(po.getApprovedBy()),
                po.getStatus(), po.getExpectedDelivery(),
                po.getTotalAmount(), po.getCreatedAt(),
                lineResponses);
    }
}