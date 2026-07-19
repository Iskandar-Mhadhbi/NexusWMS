// PickListService.java
package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEvent;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.fulfillment.dto.request.PickItemRequest;
import com.nexuswms.fulfillment.dto.response.PickListItemResponse;
import com.nexuswms.fulfillment.dto.response.PickListResponse;
import com.nexuswms.fulfillment.entity.*;
import com.nexuswms.fulfillment.repository.*;
import com.nexuswms.inventory.service.StockService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PickListService {

    private final PickListRepository pickListRepository;
    private final PickListItemRepository pickListItemRepository;
    private final FulfillmentRequestRepository fulfillmentRequestRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;  
    private final StockService stockService;
    private final WarehouseEventPublisher eventPublisher;

    /* ----- Generate Pick List ----- */
    /**
     * Generates a pick list for a given fulfillment request and assigns it to a picker.
     * One pick list item is created per order line, mapped to the first available
     * SKU location with sufficient stock.
     * Transitions the fulfillment request to IN_PROGRESS and the order to PICKING.
     *
     * @param fulfillmentRequestId the UUID of the fulfillment request
     * @param assignedTo           the UUID of the worker to assign the pick list to
     * @return the generated pick list as a response DTO
     * @throws ResourceNotFoundException if the fulfillment request does not exist
     * @throws IllegalArgumentException  if no stock location is found for a SKU
     */
    @Transactional
    public PickListResponse generate(UUID fulfillmentRequestId, UUID assignedTo, UUID generatedBy) {
        FulfillmentRequest fr = fulfillmentRequestRepository.findById(fulfillmentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fulfillment request not found with id: " + fulfillmentRequestId));

        if (fr.getStatus() != FulfillmentStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Fulfillment request is already in status: " + fr.getStatus());
        }

        List<OrderLine> orderLines = orderLineRepository.findByOrder_Id(fr.getOrder().getId());

        PickList pickList = new PickList();
        pickList.setFulfillmentRequest(fr);
        pickList.setGeneratedBy(generatedBy);
        pickList.setAssignedTo(assignedTo);
        pickList.setStatus(PickListStatus.GENERATED);
        pickListRepository.save(pickList);

        for (OrderLine line : orderLines) {
            var locationInfo = stockService.findAvailableLocationForSku(
                    line.getSkuId(), line.getQuantityOrdered());

            PickListItem item = new PickListItem();
            item.setPickList(pickList);
            item.setOrderLine(line);
            item.setSkuId(line.getSkuId());
            item.setShelfId(locationInfo.shelfId());
            item.setShelfCode(locationInfo.shelfCode());
            item.setQuantityToPick(line.getQuantityOrdered());
            item.setBatchId(locationInfo.batchId());
            item.setStatus(PickListItemStatus.PENDING);
            pickListItemRepository.save(item);

            line.setStatus(OrderLineStatus.PICKING);
            orderLineRepository.save(line);
        }

        fr.setStatus(FulfillmentStatus.IN_PROGRESS);
        fulfillmentRequestRepository.save(fr);

        fr.getOrder().setStatus(OrderStatus.PICKING);
        orderRepository.save(fr.getOrder());

        return buildPickListResponse(pickList);
    }

    /* ----- Get all Pick Lists ----- */
    /**
     * Returns all pick lists, optionally filtered by status.
     * Manager-facing oversight query — distinct from getByWorker(), which
     * scopes results to a single picker's assigned lists.
     *
     * @param status optional status filter; if null, returns all pick lists.
     * @return list of pick lists matching the filter, each with its items.
     */
    @Transactional(readOnly = true)
    public List<PickListResponse> getAll(PickListStatus status) {
        List<PickList> pickLists = (status != null)
                ? pickListRepository.findByStatus(status)
                : pickListRepository.findAll();

        return pickLists.stream()
                .map(this::buildPickListResponse) // reuse whatever conversion helper you already have for getById/getByWorker
                .toList();
    }

    /* ----- Get Pick Lists By Worker ----- */
    /**
     * Returns all pick lists assigned to a specific worker.
     *
     * @param workerId the UUID of the worker
     * @return list of pick lists assigned to that worker
     */
    @Transactional(readOnly = true)
    public List<PickListResponse> getByWorker(UUID workerId) {
        return pickListRepository.findByAssignedTo(workerId).stream()
                .map(this::buildPickListResponse)
                .collect(Collectors.toList());
    }

    /* ----- Get Pick List By ID ----- */
    /**
     * Returns a single pick list by its UUID.
     *
     * @param id the pick list UUID
     * @return the pick list as a response DTO
     * @throws ResourceNotFoundException if no pick list exists with the given ID
     */
    @Transactional(readOnly = true)
    public PickListResponse getById(UUID id) {
        PickList pickList = pickListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pick list not found with id: " + id));
        return buildPickListResponse(pickList);
    }

    /* ----- Pick Item ----- */
    /**
     * Records the picking of a specific item on a pick list.
     * Updates the item status to PICKED and updates the parent order line's quantity picked.
     * If all items on the pick list are picked, transitions the pick list to COMPLETED.
     *
     * @param pickListId the UUID of the pick list
     * @param itemId     the UUID of the pick list item
     * @param request    the quantity picked
     * @return the updated pick list item as a response DTO
     * @throws ResourceNotFoundException if the pick list item does not exist
     * @throws IllegalArgumentException  if the item is already picked or quantity exceeds required
     */
    @Transactional
    public PickListItemResponse pickItem(UUID pickListId, UUID itemId, PickItemRequest request,UUID principalUserId) {
        PickListItem item = pickListItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Pick list item not found with id: " + itemId));

        if (!item.getPickList().getId().equals(pickListId)) {
            throw new ResourceNotFoundException("Item does not belong to pick list: " + pickListId);
        }

        UUID assignedTo = item.getPickList().getAssignedTo();
        if (!assignedTo.equals(principalUserId)) {
            throw new IllegalArgumentException("You are not assigned to this pick list");
        }
        if (item.getStatus() == PickListItemStatus.PICKED) {
            throw new IllegalArgumentException("Item is already picked");
        }

        if (request.quantityPicked() > item.getQuantityToPick()) {
            throw new IllegalArgumentException(
                    "Quantity picked (" + request.quantityPicked() +
                    ") exceeds quantity to pick (" + item.getQuantityToPick() + ")");
        }

        item.setQuantityPicked(request.quantityPicked());
        item.setStatus(PickListItemStatus.PICKED);
        item.setPickedAt(LocalDateTime.now());
        item=pickListItemRepository.save(item);

        OrderLine orderLine = item.getOrderLine();
        orderLine.setQuantityPicked(orderLine.getQuantityPicked() + request.quantityPicked());
        orderLine.setStatus(OrderLineStatus.PICKED);
        orderLineRepository.save(orderLine);

        checkAndCompletePickList(item.getPickList());

        eventPublisher.publish(WarehouseEvent.of(
            "ITEM_PICKED",
            item.getId().toString(),
            item.getStatus().name(),
            principalUserId.toString(),
            "PICKING"
        ));
        
        return PickListItemResponse.from(item);
    }

    /* ----- Private Helpers ----- */

    /**
     * Checks if all items on the pick list are picked and marks it COMPLETED if so.
     */
    private void checkAndCompletePickList(PickList pickList) {
        List<PickListItem> allItems = pickListItemRepository.findByPickList_Id(pickList.getId());
        boolean allPicked = allItems.stream()
                .allMatch(i -> i.getStatus() == PickListItemStatus.PICKED
                            || i.getStatus() == PickListItemStatus.SKIPPED);
        if (allPicked) {
            pickList.setStatus(PickListStatus.COMPLETED);
            pickList.setCompletedAt(LocalDateTime.now());
            pickListRepository.save(pickList);
        }
    }

    /**
     * Builds a PickListResponse by loading items for the given pick list.
     */
    private PickListResponse buildPickListResponse(PickList pickList) {
        List<PickListItemResponse> items = pickListItemRepository
                .findByPickList_Id(pickList.getId()).stream()
                .map(PickListItemResponse::from)
                .collect(Collectors.toList());
        return PickListResponse.from(pickList, items);
    }
}