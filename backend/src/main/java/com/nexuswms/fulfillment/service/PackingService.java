// PackingService.java
package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEvent;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.fulfillment.dto.request.PackingCompleteRequest;
import com.nexuswms.fulfillment.dto.response.PackingTaskResponse;
import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.entity.*;
import com.nexuswms.fulfillment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackingService {

    private final PackingTaskRepository packingTaskRepository;
    private final PackingStationRepository packingStationRepository;
    private final PickListRepository pickListRepository;
    private final ParcelRepository parcelRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final WarehouseEventPublisher eventPublisher;

    /* ----- Create Packing Task ----- */
    /**
     * Creates a packing task from a completed pick list and assigns it to a packer.
     * Optionally assigns a packing station if one is provided.
     * The pick list must be in COMPLETED status before a packing task can be created.
     *
     * @param pickListId  the UUID of the completed pick list
     * @param assignedTo  the UUID of the packer to assign the task to
     * @param stationId   optional UUID of a packing station to assign
     * @return the created packing task as a response DTO
     * @throws ResourceNotFoundException if the pick list or station does not exist
     * @throws IllegalArgumentException  if the pick list is not yet completed
     */
    @Transactional
    public PackingTaskResponse createTask(UUID pickListId, UUID assignedTo, UUID stationId, UUID createdBy) {
        PickList pickList = pickListRepository.findById(pickListId)
                .orElseThrow(() -> new ResourceNotFoundException("Pick list not found with id: " + pickListId));

        if (pickList.getStatus() != PickListStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Cannot create packing task — pick list is not COMPLETED. Current status: "
                    + pickList.getStatus());
        }

        PackingStation station = null;
        if (stationId != null) {
            station = packingStationRepository.findById(stationId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Packing station not found with id: " + stationId));
        }

        PackingTask task = new PackingTask();
        task.setPickList(pickList);
        task.setCreatedBy(createdBy);
        task.setAssignedTo(assignedTo);
        task.setStation(station);
        task.setStatus(PackingTaskStatus.PENDING);
        packingTaskRepository.save(task);

        return PackingTaskResponse.from(task);
    }

    /* ----- Start Packing Task ----- */
    /**
     * Marks a packing task as IN_PROGRESS, records the start time, and records
     * the actual worker who started it. This may differ from assignedTo when
     * a teammate picks up a task originally assigned to someone else —
     * startedBy preserves accountability for who performed the physical action.
     *
     * @param taskId          the UUID of the packing task
     * @param principalUserId the UUID of the worker starting the task (from JWT principal)
     * @return the updated packing task as a response DTO
     * @throws ResourceNotFoundException if the task does not exist
     * @throws IllegalArgumentException  if the task is not in PENDING status   
     */
    @Transactional
    public PackingTaskResponse startTask(UUID taskId, UUID principalUserId) {
        PackingTask task = findTaskOrThrowNotFound(taskId);

        if (task.getStatus() != PackingTaskStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Task is not in PENDING status. Current status: " + task.getStatus());
        }

        task.setStatus(PackingTaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        task.setStartedBy(principalUserId);
        packingTaskRepository.save(task);

        return PackingTaskResponse.from(task);
    }
        /* ----- Complete Packing Task ----- */
        /**
         * Completes a packing task and creates a parcel with a generated tracking number and barcode.
         * Updates all order lines to PACKED status and transitions the order to PACKING.
         *
         * @param taskId  the UUID of the packing task
         * @param request the packing completion payload (weight, dimensions)
         * @return the created parcel as a response DTO
         * @throws ResourceNotFoundException if the task does not exist
         * @throws IllegalArgumentException  if the task is not IN_PROGRESS
         */
        @Transactional
        public ParcelResponse completeTask(UUID taskId, PackingCompleteRequest request,UUID principalUserId) {
            PackingTask task = findTaskOrThrowNotFound(taskId);

            if (task.getStatus() != PackingTaskStatus.IN_PROGRESS) {
                throw new IllegalArgumentException(
                        "Task is not IN_PROGRESS. Current status: " + task.getStatus());
            }

            task.setStatus(PackingTaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            packingTaskRepository.save(task);

            Order order = task.getPickList().getFulfillmentRequest().getOrder();

            Parcel parcel = new Parcel();
            parcel.setOrder(order);
            parcel.setPackingTask(task);
            parcel.setTrackingNumber(generateTrackingNumber());
            parcel.setBarcode(generateBarcode());
            parcel.setWeightKg(request.weightKg());
            parcel.setDimensions(request.dimensions());
            parcel.setStatus(PackageStatus.PACKED);
            parcelRepository.save(parcel);

            List<OrderLine> lines = orderLineRepository.findByOrder_Id(order.getId());
            lines.forEach(line -> {
                line.setQuantityPacked(line.getQuantityPicked());
                line.setStatus(OrderLineStatus.PACKED);
                orderLineRepository.save(line);
            });

            order.setStatus(OrderStatus.PACKING);
            orderRepository.save(order);

            eventPublisher.publish(WarehouseEvent.of(
                "PARCEL_PACKED",
                parcel.getId().toString(),
                parcel.getStatus().name(),
                principalUserId.toString(),
                "PACKING"
            ));
            return ParcelResponse.from(parcel);
        }

    /* ----- Get Tasks By Worker ----- */
    /**
     * Returns all packing tasks assigned to a specific worker.
     *
     * @param workerId the UUID of the worker
     * @return list of packing tasks assigned to that worker
     */
    @Transactional(readOnly = true)
    public List<PackingTaskResponse> getByWorker(UUID workerId) {
        return packingTaskRepository.findByAssignedTo(workerId).stream()
                .map(PackingTaskResponse::from)
                .collect(Collectors.toList());
    }

    /* ----- Private Helpers ----- */

    /**
     * Fetches a packing task by ID or throws ResourceNotFoundException.
     */
    private PackingTask findTaskOrThrowNotFound(UUID taskId) {
        return packingTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Packing task not found with id: " + taskId));
    }

    /**
     * Generates a unique tracking number in the format TRK-YYYYMMDD-XXXXX.
     */
    private String generateTrackingNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%05d", new Random().nextInt(100000));
        String candidate = "TRK-" + datePart + "-" + randomPart;
        return parcelRepository.existsByTrackingNumber(candidate) ? generateTrackingNumber() : candidate;
    }

    /**
     * Generates a unique barcode in the format BAR-XXXXXXXXXX (10 random digits).
     */
    private String generateBarcode() {
        String candidate = "BAR-" + String.format("%010d", new Random().nextLong(10_000_000_000L));
        return parcelRepository.existsByBarcode(candidate) ? generateBarcode() : candidate;
    }
}