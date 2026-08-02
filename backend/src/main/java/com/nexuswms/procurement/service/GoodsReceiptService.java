package com.nexuswms.procurement.service;

import com.nexuswms.procurement.dto.request.GoodsReceiptLineRequest;
import com.nexuswms.procurement.dto.request.GoodsReceiptRequest;
import com.nexuswms.procurement.dto.response.GoodsReceiptLineResponse;
import com.nexuswms.procurement.dto.response.GoodsReceiptResponse;
import com.nexuswms.procurement.entity.*;
import com.nexuswms.procurement.repository.*;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.inventory.service.StockService;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for recording physical goods receipts against
 * approved purchase orders.
 *
 * <p>receivedBy identifies the worker who physically signed for and
 * counted a delivery — enriched to a UserSummaryResponse for discrepancy
 * tracing and receiving-team accountability, matching the pattern already
 * applied to PickList/PackingTask/Shipment.</p>
 */
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    /** Maximum attempts to generate a unique GR number before giving up. */
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptLineRepository receiptLineRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final StockService stockService;
    private final UserService userService;

    /* -------------------------------------------------------------------------
     * POST /goods-receipts
     * Records physical arrival of goods against an approved PO.
     * Updates PO line quantities, PO status, and delegates stock update
     * + audit trail to StockService.
     * Only allowed on APPROVED or PARTIALLY_RECEIVED purchase orders.
     * ------------------------------------------------------------------------- */
    @Transactional
    public GoodsReceiptResponse create(GoodsReceiptRequest request, UUID receivedBy) {
        PurchaseOrder po = findApprovedPo(request.purchaseOrderId());
        Map<UUID, PurchaseOrderLine> poLineMap = getPoLineMap(po.getId());

        validateLines(request, poLineMap);

        GoodsReceipt receipt = saveReceiptHeader(request, po, receivedBy);
        processLines(request, receipt, po, poLineMap, receivedBy);
        updatePoStatus(po);

        UserSummaryResponse receivedByUser = fetchReceivedByUser(receipt);

        return toResponse(receipt, receiptLineRepository.findByGoodsReceipt_Id(receipt.getId()), po, receivedByUser);
    }

    /* -------------------------------------------------------------------------
     * GET /goods-receipts/{id}
     * Fetch a single goods receipt with its lines by ID.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public GoodsReceiptResponse getById(UUID id) {
        GoodsReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goods receipt not found: " + id));

        UserSummaryResponse receivedByUser = fetchReceivedByUser(receipt);

        return toResponse(receipt,
                receiptLineRepository.findByGoodsReceipt_Id(id),
                receipt.getPurchaseOrder(),
                receivedByUser);
    }

    /* -------------------------------------------------------------------------
     * GET /goods-receipts/by-po/{poId}
     * Fetch all goods receipts for a given purchase order.
     * A PO can have multiple receipts if goods arrive in separate deliveries.
     * Batch-fetches receivedBy user summaries to avoid N+1 query overhead.
     * ------------------------------------------------------------------------- */
    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> getByPurchaseOrder(UUID poId) {
        if (!poRepository.existsById(poId)) {
            throw new ResourceNotFoundException("Purchase order not found: " + poId);
        }

        List<GoodsReceipt> receipts = receiptRepository.findByPurchaseOrder_Id(poId);
        if (receipts.isEmpty()) return List.of();

        Set<UUID> userIds = receipts.stream()
                .map(goodsReceipt -> goodsReceipt.getReceivedBy())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryResponse> userMap = userService.getUserSummaries(userIds);

        return receipts.stream()
                .map(r -> toResponse(r,
                        receiptLineRepository.findByGoodsReceipt_Id(r.getId()),
                        r.getPurchaseOrder(),
                        userMap.get(r.getReceivedBy())))
                .toList();
    }

    /* -------------------------------------------------------------------------
     * Validates that the PO exists and is in a receivable state.
     * ------------------------------------------------------------------------- */
    private PurchaseOrder findApprovedPo(UUID poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + poId));
        if (po.getStatus() != PurchaseOrderStatus.APPROVED
                && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalArgumentException(
                    "Goods can only be received against APPROVED or PARTIALLY_RECEIVED purchase orders");
        }
        return po;
    }

    /* -------------------------------------------------------------------------
     * Fetches all PO lines and indexes them by ID into a Map for O(1) lookup
     * during line validation and processing — avoids O(n²) list scanning.
     * ------------------------------------------------------------------------- */
    private Map<UUID, PurchaseOrderLine> getPoLineMap(UUID poId) {
        return poLineRepository.findByPurchaseOrder_Id(poId).stream()
                .collect(Collectors.toMap(
                        line -> Objects.requireNonNull(line.getId()),
                        line -> line
                ));
    }

    /* -------------------------------------------------------------------------
     * Validates all receipt lines before any writes happen.
     * Checks: PO line exists, SKU matches, quantity doesn't exceed remaining.
     * Fails fast — if any line is invalid, nothing gets saved.
     * ------------------------------------------------------------------------- */
    private void validateLines(GoodsReceiptRequest request, Map<UUID, PurchaseOrderLine> poLineMap) {
        Set<UUID> seenPoLineIds = new HashSet<>();
        for (GoodsReceiptLineRequest line : request.lines()) {
            PurchaseOrderLine poLine = poLineMap.get(line.poLineId());
            if (poLine == null) {
                throw new ResourceNotFoundException("PO line not found: " + line.poLineId());
            }
            if (!seenPoLineIds.add(line.poLineId())) {
                throw new IllegalArgumentException(
                        "Duplicate PO line in the same receipt: " + line.poLineId());
            }
            if (!poLine.getSkuId().equals(line.skuId())) {
                throw new IllegalArgumentException("SKU mismatch on PO line: " + line.poLineId());
            }
            int remaining = poLine.getQuantityOrdered() - poLine.getQuantityReceived();
            if (line.quantityReceived() > remaining) {
                throw new IllegalArgumentException(
                        "Quantity received (" + line.quantityReceived()
                        + ") exceeds remaining (" + remaining
                        + ") for PO line: " + line.poLineId());
            }
        }
    }

    /* -------------------------------------------------------------------------
     * Saves the goods receipt header (PO reference, receiver, notes).
     * Lines are saved separately in processLines.
     * ------------------------------------------------------------------------- */
    private GoodsReceipt saveReceiptHeader(GoodsReceiptRequest request, PurchaseOrder po, UUID receivedBy) {
        return receiptRepository.save(GoodsReceipt.builder()
                .grNumber(generateGrNumber())
                .purchaseOrder(po)
                .receivedBy(receivedBy)
                .notes(request.notes())
                .build());
    }

    /* -------------------------------------------------------------------------
     * Processes each receipt line:
     *   1. Saves the GoodsReceiptLine record
     *   2. Updates the PO line received quantity and status
     *   3. Delegates stock level update + audit trail to StockService
     * ------------------------------------------------------------------------- */
    private void processLines(GoodsReceiptRequest request, GoodsReceipt receipt,
                               PurchaseOrder po, Map<UUID, PurchaseOrderLine> poLineMap,
                               UUID receivedBy) {
        for (GoodsReceiptLineRequest lineReq : request.lines()) {
            PurchaseOrderLine poLine = poLineMap.get(lineReq.poLineId());

            receiptLineRepository.save(GoodsReceiptLine.builder()
                    .goodsReceipt(receipt)
                    .poLineId(lineReq.poLineId())
                    .skuId(lineReq.skuId())
                    .quantityReceived(lineReq.quantityReceived())
                    .batchId(lineReq.batchId())
                    .expiryDate(lineReq.expiryDate())
                    .shelfId(lineReq.shelfId())
                    .build());

            updatePoLine(poLine, lineReq.quantityReceived());

            stockService.adjustFromGoodsReceipt(
                    lineReq.skuId(),
                    lineReq.shelfId(),
                    lineReq.quantityReceived(),
                    lineReq.batchId(),
                    lineReq.expiryDate(),
                    receivedBy,
                    "Goods receipt: " + po.getPoNumber());
        }
    }

    /* -------------------------------------------------------------------------
     * Updates a single PO line's received quantity and derives its status.
     * PARTIALLY_RECEIVED if some remain, FULLY_RECEIVED if all accounted for.
     * ------------------------------------------------------------------------- */
    private void updatePoLine(PurchaseOrderLine poLine, int quantityReceived) {
        poLine.setQuantityReceived(poLine.getQuantityReceived() + quantityReceived);
        poLine.setStatus(poLine.getQuantityReceived() >= poLine.getQuantityOrdered()
                ? PurchaseOrderLineStatus.FULLY_RECEIVED
                : PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
        poLineRepository.save(poLine);
    }

    /* -------------------------------------------------------------------------
     * Recalculates and updates the parent PO status after all lines are processed.
     * FULLY_RECEIVED only if every line is fully received, otherwise PARTIALLY_RECEIVED.
     * ------------------------------------------------------------------------- */
    private void updatePoStatus(PurchaseOrder po) {
        List<PurchaseOrderLine> allLines = poLineRepository.findByPurchaseOrder_Id(po.getId());
        boolean allDone = allLines.stream()
                .allMatch(l -> l.getStatus() == PurchaseOrderLineStatus.FULLY_RECEIVED);
        po.setStatus(allDone
                ? PurchaseOrderStatus.FULLY_RECEIVED
                : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        poRepository.save(po);
    }

    /* -------------------------------------------------------------------------
     * Resolves the user summary for a receipt's receiver. Returns null if
     * receivedBy is null rather than throwing — receivedBy is nullable on
     * the entity, so enrichment must degrade gracefully, never block a read.
     * ------------------------------------------------------------------------- */
    private UserSummaryResponse fetchReceivedByUser(GoodsReceipt receipt) {
        UUID receivedBy = receipt.getReceivedBy();
        if (receivedBy == null) return null;
        return userService.getUserSummaries(Set.of(receivedBy)).get(receivedBy);
    }

    /* -------------------------------------------------------------------------
     * Maps GoodsReceipt + lines to the response DTO.
     * skuCode and shelfCode are null here — resolved by the frontend via IDs.
     * ------------------------------------------------------------------------- */
    private GoodsReceiptResponse toResponse(GoodsReceipt receipt,
                                             List<GoodsReceiptLine> lines,
                                             PurchaseOrder po,
                                             UserSummaryResponse receivedByUser) {
        List<GoodsReceiptLineResponse> lineResponses = lines.stream()
                .map(l -> new GoodsReceiptLineResponse(
                        l.getId(), l.getPoLineId(), l.getSkuId(),
                        null, l.getQuantityReceived(),
                        l.getBatchId(), l.getExpiryDate(),
                        l.getShelfId(), null))
                .toList();
        return new GoodsReceiptResponse(
                receipt.getId(), receipt.getGrNumber(),
                po.getId(), po.getPoNumber(),
                receivedByUser, receipt.getReceivedAt(),
                receipt.getNotes(), lineResponses);
    }

    /* -------------------------------------------------------------------------
     * Generates a unique GR number in format GR-YYYYMMDD-XXXXX.
     * Bounded retry on collision — throws after MAX_GENERATION_ATTEMPTS
     * rather than looping indefinitely, consistent with Order/PackingTask/
     * Shipment number generation.
     * ------------------------------------------------------------------------- */
    private String generateGrNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "GR-" + datePart + "-" + String.format("%05d", new Random().nextInt(100000));
            if (!receiptRepository.existsByGrNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique GR number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}