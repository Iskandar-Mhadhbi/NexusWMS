// PackingService.java
package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEvent;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.fulfillment.document.PackingTaskEvent;
import com.nexuswms.fulfillment.dto.request.PackingCompleteRequest;
import com.nexuswms.fulfillment.dto.response.PackingTaskResponse;
import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.entity.*;
import com.nexuswms.fulfillment.repository.*;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
 * Service responsible for packing task lifecycle: creation, starting, and
 * completion (which produces a Parcel).
 *
 * <p>Every lifecycle transition is recorded twice: an immutable
 * {@link PackingTaskEvent} in MongoDB (audit trail) and a
 * {@link WarehouseEvent} published to Redis pub/sub (live dashboard feed).
 * Both are written together via {@link #recordLifecycleEvent}, so the two
 * trails never drift apart.</p>
 */
@Service
@RequiredArgsConstructor
public class PackingService {

    /** Maximum attempts to generate a unique task number / tracking number / barcode before giving up. */
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final PackingTaskRepository packingTaskRepository;
    private final PackingStationRepository packingStationRepository;
    private final PickListRepository pickListRepository;
    private final ParcelRepository parcelRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final UserService userService;
    private final WarehouseEventPublisher eventPublisher;
    private final PackingTaskEventRepository packingTaskEventRepository;

    /* ----- Create Packing Task ----- */

    /**
     * Creates a packing task from a completed pick list and assigns it to a packer.
     * Optionally assigns a packing station if one is provided.
     * The pick list must be in COMPLETED status before a packing task can be created.
     * Task number is auto-generated in format PT-YYYYMMDD-XXXXX.
     *
     * @param pickListId the UUID of the completed pick list
     * @param assignedTo the UUID of the packer to assign the task to
     * @param stationId  optional UUID of a packing station to assign
     * @param createdBy  the UUID of the manager creating the task
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
        task.setTaskNumber(generateTaskNumber());
        task.setCreatedBy(createdBy);
        task.setAssignedTo(assignedTo);
        task.setStation(station);
        task.setStatus(PackingTaskStatus.PENDING);
        task = packingTaskRepository.save(task);

        recordLifecycleEvent(task, "PACKING_TASK_CREATED", createdBy, "PACKING");

        return enrichAndConvertToResponse(task);
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
        task = packingTaskRepository.save(task);

        recordLifecycleEvent(task, "PACKING_TASK_STARTED", principalUserId, "PACKING");

        return enrichAndConvertToResponse(task);
    }

    /* ----- Complete Packing Task ----- */

    /**
     * Completes a packing task and creates a parcel with a generated tracking number and barcode.
     * Updates all order lines to PACKED status and transitions the order to PACKING.
     *
     * @param taskId          the UUID of the packing task
     * @param request         the packing completion payload (weight, dimensions)
     * @param principalUserId the UUID of the worker completing the task
     * @return the created parcel as a response DTO
     * @throws ResourceNotFoundException if the task does not exist
     * @throws IllegalArgumentException  if the task is not IN_PROGRESS
     */
    @Transactional
    public ParcelResponse completeTask(UUID taskId, PackingCompleteRequest request, UUID principalUserId) {
        PackingTask task = findTaskOrThrowNotFound(taskId);

        if (task.getStatus() != PackingTaskStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "Task is not IN_PROGRESS. Current status: " + task.getStatus());
        }

        task.setStatus(PackingTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task = packingTaskRepository.save(task);

        recordLifecycleEvent(task, "PACKING_TASK_COMPLETED", principalUserId, "PACKING");

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

    /* ----- Get All Tasks ----- */

    /**
     * Returns all packing tasks, optionally filtered by status.
     * Manager-facing oversight query — distinct from getByWorker(), which
     * scopes results to a single packer's assigned tasks.
     * Batch-fetches assignedTo/startedBy user summaries to avoid N+1 query overhead.
     *
     * @param status optional status filter; if null, returns all packing tasks.
     * @return list of packing tasks matching the filter.
     */
    @Transactional(readOnly = true)
    public List<PackingTaskResponse> getAll(PackingTaskStatus status) {
        List<PackingTask> tasks = (status != null)
                ? packingTaskRepository.findByStatus(status)
                : packingTaskRepository.findAll();

        if (tasks.isEmpty()) return List.of();

        Map<UUID, UserSummaryResponse> userMap = batchFetchUserSummaries(tasks);

        return tasks.stream()
                .map(task -> PackingTaskResponse.from(
                        task,
                        userMap.get(task.getAssignedTo()),
                        userMap.get(task.getStartedBy())
                ))
                .toList();
    }

    /* ----- Get Tasks By Worker ----- */

    /**
     * Returns all packing tasks assigned to a specific worker.
     * Batch-fetches assignedTo/startedBy user summaries to avoid N+1 query overhead.
     *
     * @param workerId the UUID of the worker
     * @return list of packing tasks assigned to that worker
     */
    @Transactional(readOnly = true)
    public List<PackingTaskResponse> getByWorker(UUID workerId) {
        List<PackingTask> tasks = packingTaskRepository.findByAssignedTo(workerId);
        if (tasks.isEmpty()) return List.of();

        Map<UUID, UserSummaryResponse> userMap = batchFetchUserSummaries(tasks);

        return tasks.stream()
                .map(task -> PackingTaskResponse.from(
                        task,
                        userMap.get(task.getAssignedTo()),
                        userMap.get(task.getStartedBy())
                ))
                .toList();
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
     * Batch-fetches user summaries for a list of tasks' assignedTo and
     * startedBy fields in a single call, avoiding N+1 queries.
     */
    private Map<UUID, UserSummaryResponse> batchFetchUserSummaries(List<PackingTask> tasks) {
        Set<UUID> userIds = tasks.stream()
                .flatMap(task -> Stream.of(task.getAssignedTo(), task.getStartedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return userService.getUserSummaries(userIds);
    }

    /**
     * Builds a PackingTaskResponse for a single task, resolving its
     * assignedTo/startedBy user summaries in one batched call.
     */
    private PackingTaskResponse enrichAndConvertToResponse(PackingTask task) {
        Map<UUID, UserSummaryResponse> userMap = batchFetchUserSummaries(List.of(task));
        return PackingTaskResponse.from(
                task,
                userMap.get(task.getAssignedTo()),
                userMap.get(task.getStartedBy())
        );
    }

    /**
     * Writes both halves of a packing task lifecycle event: the immutable
     * MongoDB audit record and the live Redis pub/sub event for the dashboard.
     */
    private void recordLifecycleEvent(PackingTask task, String eventType, UUID actorId, String zone) {
        eventPublisher.publish(WarehouseEvent.of(
                eventType,
                task.getId().toString(),
                task.getStatus().name(),
                actorId.toString(),
                zone
        ));
        packingTaskEventRepository.save(new PackingTaskEvent(
                task.getId().toString(), task.getTaskNumber(),
                eventType, task.getStatus().name(), actorId.toString()
        ));
    }

    /**
     * Generates a unique task number in the format PT-YYYYMMDD-XXXXX.
     * Bounded retry on collision — throws after MAX_GENERATION_ATTEMPTS
     * rather than recursing indefinitely.
     */
    private String generateTaskNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "PT-" + datePart + "-" + String.format("%05d", new Random().nextInt(100000));
            if (!packingTaskRepository.existsByTaskNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique task number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * Generates a unique tracking number in the format TRK-YYYYMMDD-XXXXX.
     * Bounded retry on collision — throws after MAX_GENERATION_ATTEMPTS
     * rather than recursing indefinitely.
     */
    private String generateTrackingNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "TRK-" + datePart + "-" + String.format("%05d", new Random().nextInt(100000));
            if (!parcelRepository.existsByTrackingNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique tracking number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * Generates a unique barcode in the format BAR-XXXXXXXXXX (10 random digits).
     * Bounded retry on collision — throws after MAX_GENERATION_ATTEMPTS
     * rather than recursing indefinitely.
     */
    private String generateBarcode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "BAR-" + String.format("%010d", new Random().nextLong(10_000_000_000L));
            if (!parcelRepository.existsByBarcode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique barcode after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}