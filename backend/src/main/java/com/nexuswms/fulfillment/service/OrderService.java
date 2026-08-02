// OrderService.java
package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEvent;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.fulfillment.document.OrderEvent;
import com.nexuswms.fulfillment.dto.request.OrderRequest;
import com.nexuswms.fulfillment.dto.response.FulfillmentRequestResponse;
import com.nexuswms.fulfillment.dto.response.OrderLineResponse;
import com.nexuswms.fulfillment.dto.response.OrderResponse;
import com.nexuswms.fulfillment.entity.*;
import com.nexuswms.fulfillment.repository.*;
import com.nexuswms.inventory.dto.response.SkuResponse;
import com.nexuswms.inventory.service.SkuService;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for order lifecycle management: creation, cancellation,
 * and fulfillment-request generation.
 *
 * <p>Order lifecycle changes are recorded twice, for two different purposes:
 * an immutable {@link OrderEvent} written to MongoDB (audit trail), and a
 * {@link WarehouseEvent} published to Redis pub/sub (live dashboard feed).
 * Every status-changing action publishes both — the dashboard should always
 * reflect the same lifecycle the audit trail records.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    /** Maximum attempts to generate a unique order number before giving up. */
    private static final int MAX_ORDER_NUMBER_ATTEMPTS = 10;

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final FulfillmentRequestRepository fulfillmentRequestRepository;
    private final SkuService skuService;
    private final UserService userService;
    private final WarehouseEventPublisher eventPublisher;
    private final OrderEventRepository orderEventRepository;

    /* ----- Create Order ----- */

    /**
     * Creates a new order with its lines.
     * Order number is auto-generated in format ORD-YYYYMMDD-XXXXX.
     * All SKU IDs in the lines are validated via SkuService before persisting.
     *
     * @param request   the order creation payload
     * @param createdBy the UUID of the manager/admin creating the order
     * @return the persisted order as a response DTO
     */
    @Transactional
    public OrderResponse create(OrderRequest request, UUID createdBy) {
        String orderNumber = generateOrderNumber();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCreatedBy(createdBy);
        order.setCustomerName(request.customerName());
        order.setCustomerAddress(request.customerAddress());
        order.setNotes(request.notes());
        order.setPriority(parsePriority(request.priority()));
        order.setStatus(OrderStatus.RECEIVED);
        order = orderRepository.save(order);

        Set<UUID> skuIds = request.lines().stream()
                .map(l -> Objects.requireNonNull(l.skuId()))
                .collect(Collectors.toSet());
        Map<UUID, SkuResponse> skuMap = skuService.getSkuResponseMapByIds(skuIds);

        List<OrderLine> lines = new ArrayList<>();
        for (var lineReq : request.lines()) {
            if (!skuMap.containsKey(lineReq.skuId())) {
                throw new ResourceNotFoundException("SKU not found with id: " + lineReq.skuId());
            }
            OrderLine line = new OrderLine();
            line.setOrder(order);
            line.setSkuId(lineReq.skuId());
            line.setQuantityOrdered(lineReq.quantityOrdered());
            line.setUnitPrice(lineReq.unitPrice());
            line.setStatus(OrderLineStatus.PENDING);
            lines.add(line);
        }
        orderLineRepository.saveAll(lines);

        recordLifecycleEvent(order, "ORDER_CREATED", createdBy, "OMS");

        UserSummaryResponse createdByUser = userService.getUserSummaries(Set.of(createdBy))
                .get(createdBy);

        return enrichAndConvertToResponse(order, lines, skuMap, createdByUser);
    }

    /* ----- Get All Orders ----- */

    /**
     * Returns all orders in the system.
     * Batch-fetches associated OrderLines, SKU metadata, and creator user
     * summaries to avoid N+1 query overhead.
     *
     * @return list of all orders as response DTOs
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) return Collections.emptyList();

        List<UUID> orderIds = orders.stream()
                .map(order -> order.getId())
                .toList();

        List<OrderLine> allLines = orderLineRepository.findByOrder_IdIn(orderIds);
        Map<UUID, List<OrderLine>> linesByOrderId = allLines.stream()
                .collect(Collectors.groupingBy(line -> line.getOrder().getId()));

        Set<UUID> allSkuIds = allLines.stream()
                .map(orderLine -> orderLine.getSkuId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, SkuResponse> skuMap = allSkuIds.isEmpty()
                ? Collections.emptyMap()
                : skuService.getSkuResponseMapByIds(allSkuIds);

        Set<UUID> creatorIds = orders.stream()
                .map(order -> order.getCreatedBy())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserSummaryResponse> userMap = userService.getUserSummaries(creatorIds);

        return orders.stream()
                .map(order -> {
                    List<OrderLine> lines = linesByOrderId.getOrDefault(order.getId(), Collections.emptyList());
                    return enrichAndConvertToResponse(order, lines, skuMap, userMap.get(order.getCreatedBy()));
                })
                .toList();
    }

    /* ----- Get Order By ID ----- */

    /**
     * Returns a single order by its UUID.
     *
     * @param id the order UUID
     * @return the order as a response DTO
     * @throws ResourceNotFoundException if no order exists with the given ID
     */
    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        Order order = findOrderOrThrow(id);
        List<OrderLine> lines = orderLineRepository.findByOrder_Id(id);

        Map<UUID, SkuResponse> skuMap = fetchSkuMapForLines(lines);
        UserSummaryResponse createdByUser = fetchCreatedByUser(order);

        return enrichAndConvertToResponse(order, lines, skuMap, createdByUser);
    }

    /* ----- Cancel Order ----- */

    /**
     * Cancels an order. Only orders in RECEIVED or VALIDATED status can be cancelled.
     * Orders already in picking, packing, or dispatched cannot be cancelled.
     *
     * @param id               the order UUID
     * @param principalUserId  the UUID of the user performing the cancellation
     * @return the updated order as a response DTO
     * @throws ResourceNotFoundException if no order exists with the given ID
     * @throws IllegalArgumentException  if the order is not in a cancellable state
     */
    @Transactional
    public OrderResponse cancel(UUID id, UUID principalUserId) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.RECEIVED && order.getStatus() != OrderStatus.VALIDATED) {
            throw new IllegalArgumentException(
                    "Cannot cancel order in status: " + order.getStatus() +
                    ". Only RECEIVED or VALIDATED orders can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        recordLifecycleEvent(order, "ORDER_CANCELLED", principalUserId, "OMS");

        List<OrderLine> lines = orderLineRepository.findByOrder_Id(id);
        Map<UUID, SkuResponse> skuMap = fetchSkuMapForLines(lines);
        UserSummaryResponse createdByUser = fetchCreatedByUser(order);

        return enrichAndConvertToResponse(order, lines, skuMap, createdByUser);
    }

    /* ----- Generate Fulfillment Request ----- */

    /**
     * Validates an order and generates a fulfillment request for it.
     * Transitions the order from RECEIVED to VALIDATED.
     * Only one fulfillment request can exist per order.
     *
     * @param id          the order UUID
     * @param generatedBy the UUID of the manager/admin generating the request
     * @return the created fulfillment request as a response DTO
     * @throws ResourceNotFoundException if no order exists with the given ID
     * @throws IllegalArgumentException  if the order is not in RECEIVED status
     * @throws ConflictException         if a fulfillment request already exists for this order
     */
    @Transactional
    public FulfillmentRequestResponse generateFulfillmentRequest(UUID id, UUID generatedBy) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new IllegalArgumentException(
                    "Cannot generate fulfillment request for order in status: " + order.getStatus());
        }
        fulfillmentRequestRepository.findByOrder_Id(id).ifPresent(fr -> {
            throw new ConflictException("Fulfillment request already exists for order: " + order.getOrderNumber());
        });

        order.setStatus(OrderStatus.VALIDATED);
        Order savedOrder = orderRepository.save(order);

        recordLifecycleEvent(savedOrder, "ORDER_VALIDATED", generatedBy, "OMS");

        FulfillmentRequest fr = new FulfillmentRequest();
        fr.setOrder(savedOrder);
        fr.setGeneratedBy(generatedBy);
        fr.setStatus(FulfillmentStatus.PENDING);
        fulfillmentRequestRepository.save(fr);

        return FulfillmentRequestResponse.from(fr);
    }

    /* ----- Private Helpers ----- */

    /**
     * Fetches an order by ID or throws ResourceNotFoundException.
     */
    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    /**
     * Batch-fetches SKU metadata for a set of order lines. Returns an empty
     * map (not a lookup exception) when there are no lines, since an order
     * with zero lines is a valid — if unusual — state to render.
     */
    private Map<UUID, SkuResponse> fetchSkuMapForLines(List<OrderLine> lines) {
        Set<UUID> skuIds = lines.stream()
                .map(l -> Objects.requireNonNull(l.getSkuId()))
                .collect(Collectors.toSet());
        return skuIds.isEmpty()
                ? Collections.emptyMap()
                : skuService.getSkuResponseMapByIds(skuIds);
    }

    /**
     * Resolves the user summary for an order's creator. Returns null if
     * createdBy is null (legacy data) rather than throwing — enrichment
     * failures should degrade gracefully, never block a read.
     */
    private UserSummaryResponse fetchCreatedByUser(Order order) {
        if (order.getCreatedBy() == null) return null;
        return userService.getUserSummaries(Set.of(order.getCreatedBy())).get(order.getCreatedBy());
    }

    /**
     * Writes both halves of an order lifecycle event: the immutable MongoDB
     * audit record and the live Redis pub/sub event for the dashboard.
     * Every status-changing action calls this exactly once, so the two
     * trails never drift apart.
     */
    private void recordLifecycleEvent(Order order, String eventType, UUID actorId, String zone) {
        eventPublisher.publish(WarehouseEvent.of(
                eventType,
                order.getId().toString(),
                order.getStatus().name(),
                actorId.toString(),
                zone
        ));
        orderEventRepository.save(new OrderEvent(
                order.getId().toString(), order.getOrderNumber(),
                eventType, order.getStatus().name(), actorId.toString()
        ));
    }

    /**
     * Builds an OrderResponse by enriching lines with SKU metadata and the
     * order with its creator's user summary.
     */
    private OrderResponse enrichAndConvertToResponse(Order order, List<OrderLine> lines,
                                                       Map<UUID, SkuResponse> skuMap,
                                                       UserSummaryResponse createdByUser) {
        List<OrderLineResponse> lineResponses = lines.stream()
                .map(line -> {
                    SkuResponse sku = skuMap.get(line.getSkuId());
                    String skuCode = sku != null ? sku.skuCode() : null;
                    String skuName = sku != null ? sku.name() : null;
                    return OrderLineResponse.from(line, skuCode, skuName);
                })
                .toList();
        return OrderResponse.from(order, lineResponses, createdByUser);
    }

    /**
     * Generates a unique order number in the format ORD-YYYYMMDD-XXXXX.
     * Retries on collision up to MAX_ORDER_NUMBER_ATTEMPTS times before
     * failing loudly — a bounded loop rather than unbounded recursion, so a
     * pathological collision run surfaces as a clear error instead of a
     * stack overflow.
     */
    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < MAX_ORDER_NUMBER_ATTEMPTS; attempt++) {
            String candidate = "ORD-" + datePart + "-" + String.format("%05d", new Random().nextInt(100000));
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique order number after " + MAX_ORDER_NUMBER_ATTEMPTS + " attempts");
    }

    /**
     * Parses priority string to enum, defaulting to STANDARD if null or unrecognized.
     */
    private OrderPriority parsePriority(String priority) {
        if (priority == null) return OrderPriority.STANDARD;
        try {
            return OrderPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OrderPriority.STANDARD;
        }
    }
}