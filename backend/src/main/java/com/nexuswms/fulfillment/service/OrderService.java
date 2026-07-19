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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final FulfillmentRequestRepository fulfillmentRequestRepository;
    private final SkuService skuService;
    private final WarehouseEventPublisher eventPublisher; 
    private final OrderEventRepository orderEventRepository;
    /* ----- Create Order ----- */
    /**
     * Creates a new order with its lines.
     * Order number is auto-generated in format ORD-YYYYMMDD-XXXXX.
     * All SKU IDs in the lines are validated via SkuService before persisting.
     *
     * @param request the order creation payload
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
        order=orderRepository.save(order);

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

        eventPublisher.publish(WarehouseEvent.of(
            "ORDER_CREATED",
            order .getId().toString(),
            order .getStatus().name(),
            createdBy.toString(),
            "OMS"
        ));
        orderEventRepository.save(new OrderEvent(
            order.getId().toString(), order.getOrderNumber(),
            "ORDER_CREATED", order.getStatus().name(), createdBy.toString()
        ));
        return enrichAndConvertToResponse(order, lines, skuMap);
    }

    /* ----- Get All Orders ----- */
    /**
     * Returns all orders in the system.
     * Optionally filterable by status in future iterations.
     *
     * @return list of all orders as response DTOs
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(order -> {
                    List<OrderLine> lines = orderLineRepository.findByOrder_Id(order.getId());
                    Set<UUID> skuIds = lines.stream()
                            .map(l -> Objects.requireNonNull(l.getSkuId()))
                            .collect(Collectors.toSet());
                    Map<UUID, SkuResponse> skuMap = skuIds.isEmpty()
                            ? Collections.emptyMap()
                            : skuService.getSkuResponseMapByIds(skuIds);
                    return enrichAndConvertToResponse(order, lines, skuMap);
                })
                .collect(Collectors.toList());
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
        Order order = orderRepository.findById(id)
                                     .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        List<OrderLine> lines = orderLineRepository.findByOrder_Id(id);
        Set<UUID> skuIds = lines.stream()
                .map(l -> Objects.requireNonNull(l.getSkuId()))
                .collect(Collectors.toSet());
        Map<UUID, SkuResponse> skuMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : skuService.getSkuResponseMapByIds(skuIds);
        return enrichAndConvertToResponse(order, lines, skuMap);
    }

    /* ----- Cancel Order ----- */
    /**
     * Cancels an order. Only orders in RECEIVED or VALIDATED status can be cancelled.
     * Orders already in picking, packing, or dispatched cannot be cancelled.
     *
     * @param id the order UUID
     * @return the updated order as a response DTO
     * @throws ResourceNotFoundException if no order exists with the given ID
     * @throws IllegalArgumentException  if the order is not in a cancellable state
     */
    @Transactional
    public OrderResponse cancel(UUID id,UUID principalUserId) {
        Order order = orderRepository.findById(id)
                                     .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.RECEIVED && order.getStatus() != OrderStatus.VALIDATED) {
            throw new IllegalArgumentException(
                    "Cannot cancel order in status: " + order.getStatus() +
                    ". Only RECEIVED or VALIDATED orders can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order=orderRepository.save(order);
        orderEventRepository.save(new OrderEvent(
            order.getId().toString(), order.getOrderNumber(),
            "ORDER_CANCELLED", order.getStatus().name(), principalUserId.toString()
        )); 
        List<OrderLine> lines = orderLineRepository.findByOrder_Id(id);
        Set<UUID> skuIds = lines.stream()
                .map(l -> Objects.requireNonNull(l.getSkuId()))
                .collect(Collectors.toSet());
        Map<UUID, SkuResponse> skuMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : skuService.getSkuResponseMapByIds(skuIds);

      
        return enrichAndConvertToResponse(order, lines, skuMap);
    }

    /* ----- Generate Fulfillment Request ----- */
    /**
     * Validates an order and generates a fulfillment request for it.
     * Transitions the order from RECEIVED to VALIDATED.
     * Only one fulfillment request can exist per order.
     *
     * @param id the order UUID
     * @return the created fulfillment request as a response DTO
     * @throws ResourceNotFoundException if no order exists with the given ID
     * @throws IllegalArgumentException  if the order is not in RECEIVED status
     * @throws ConflictException         if a fulfillment request already exists for this order
     */
    @Transactional
    public FulfillmentRequestResponse generateFulfillmentRequest(UUID id, UUID generatedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        if (order.getStatus() != OrderStatus.RECEIVED) {
            throw new IllegalArgumentException("Cannot generate fulfillment request for order in status: " + order.getStatus());
        }
        fulfillmentRequestRepository.findByOrder_Id(id).ifPresent(fr -> {
            throw new ConflictException("Fulfillment request already exists for order: " + order.getOrderNumber());
        });

        order.setStatus(OrderStatus.VALIDATED);
        Order savedOrder=orderRepository.save(order);
        orderEventRepository.save(new OrderEvent(
            savedOrder.getId().toString(), savedOrder.getOrderNumber(),
            "ORDER_VALIDATED", savedOrder.getStatus().name(), generatedBy.toString()
        ));

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

    /**
     * Builds an OrderResponse by enriching lines with SKU metadata from the provided map.
     */
    private OrderResponse enrichAndConvertToResponse(Order order, List<OrderLine> lines,
                                                      Map<UUID, SkuResponse> skuMap) {
        List<OrderLineResponse> lineResponses = lines.stream()
                .map(line -> {
                    SkuResponse sku = skuMap.get(line.getSkuId());
                    String skuCode = sku != null ? sku.skuCode() : null;
                    String skuName = sku != null ? sku.name() : null;
                    return OrderLineResponse.from(line, skuCode, skuName);
                })
                .collect(Collectors.toList());
        return OrderResponse.from(order, lineResponses);
    }

    /**
     * Generates a unique order number in the format ORD-YYYYMMDD-XXXXX.
     */
    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%05d", new Random().nextInt(100000));
        String candidate = "ORD-" + datePart + "-" + randomPart;
        return orderRepository.existsByOrderNumber(candidate) ? generateOrderNumber() : candidate;
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