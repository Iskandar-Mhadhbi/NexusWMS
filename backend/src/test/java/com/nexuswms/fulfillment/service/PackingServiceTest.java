package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.fulfillment.document.PackingTaskEvent;
import com.nexuswms.fulfillment.dto.request.PackingCompleteRequest;
import com.nexuswms.fulfillment.dto.response.PackingTaskResponse;
import com.nexuswms.fulfillment.dto.response.ParcelResponse;
import com.nexuswms.fulfillment.entity.*;
import com.nexuswms.fulfillment.repository.*;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith; 
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; 
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PackingService — createTask(), startTask(), completeTask(),
 * getAll(), getByWorker(). All repositories/services mocked; no database.
 */
@ExtendWith(MockitoExtension.class)
class PackingServiceTest {

    @Mock private PackingTaskRepository packingTaskRepository;
    @Mock private PackingStationRepository packingStationRepository;
    @Mock private PickListRepository pickListRepository;
    @Mock private ParcelRepository parcelRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderLineRepository orderLineRepository;
    @Mock private UserService userService;
    @Mock private WarehouseEventPublisher eventPublisher;
    @Mock private PackingTaskEventRepository packingTaskEventRepository;

    private PackingService packingService;

    private UUID createdById;
    private UUID assignedToId;
    private UUID startedById;
    private PickList pickList;
    private PackingTask task;
    private Order order;
    private OrderLine orderLine;

    @BeforeEach
    void setUp() {
        packingService = new PackingService(
                packingTaskRepository, packingStationRepository, pickListRepository,
                parcelRepository, orderRepository, orderLineRepository,
                userService, eventPublisher, packingTaskEventRepository);

        createdById = UUID.randomUUID();
        assignedToId = UUID.randomUUID();
        startedById = UUID.randomUUID();

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNumber("ORD-20260803-00001");
        order.setStatus(OrderStatus.PICKING);

        orderLine = new OrderLine();
        orderLine.setId(UUID.randomUUID());
        orderLine.setOrder(order);
        orderLine.setQuantityOrdered(5);
        orderLine.setQuantityPicked(5);
        orderLine.setStatus(OrderLineStatus.PICKED);
        order.getLines().add(orderLine);

        FulfillmentRequest fr = new FulfillmentRequest();
        fr.setId(UUID.randomUUID());
        fr.setOrder(order);

        pickList = new PickList();
        pickList.setId(UUID.randomUUID());
        pickList.setFulfillmentRequest(fr);
        pickList.setStatus(PickListStatus.COMPLETED);

        task = new PackingTask();
        task.setId(UUID.randomUUID());
        task.setPickList(pickList);
        task.setTaskNumber("PT-20260803-00001");
        task.setCreatedBy(createdById);
        task.setAssignedTo(assignedToId);
        task.setStatus(PackingTaskStatus.PENDING);
    }

    /** Builds a stub UserSummaryResponse map for the given IDs, for stubbing userService.getUserSummaries(). */
    private Map<UUID, UserSummaryResponse> userSummaryMapFor(UUID... ids) {
        Map<UUID, UserSummaryResponse> map = new HashMap<>();
        for (UUID id : ids) {
            map.put(id, new UserSummaryResponse(id, "EMP-" + id.toString().substring(0, 4), "Test", "test@x.com", null, null));
        }
        return map;
    }

    @Nested
    class CreateTask {

        @Test
        void createsTask_whenPickListCompleted() {
            when(pickListRepository.findById(pickList.getId())).thenReturn(Optional.of(pickList));
            when(packingTaskRepository.existsByTaskNumber(any())).thenReturn(false);
            when(packingTaskRepository.save(any(PackingTask.class))).thenAnswer(inv -> {
                                                                                        PackingTask t = inv.getArgument(0);
                                                                                        t.setId(UUID.randomUUID());
                                                                                        return t;
});
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(assignedToId));

            PackingTaskResponse result = packingService.createTask(pickList.getId(), assignedToId, null, createdById);

            assertThat(result.status()).isEqualTo(PackingTaskStatus.PENDING);
            assertThat(result.taskNumber()).startsWith("PT-");
            verify(eventPublisher).publish(any());
            verify(packingTaskEventRepository).save(any(PackingTaskEvent.class));
        }

        @Test
        void throwsIllegalArgument_whenPickListNotCompleted() {
            pickList.setStatus(PickListStatus.IN_PROGRESS);
            when(pickListRepository.findById(pickList.getId())).thenReturn(Optional.of(pickList));

            assertThatThrownBy(() ->
                    packingService.createTask(pickList.getId(), assignedToId, null, createdById))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not COMPLETED");

            verifyNoInteractions(packingTaskRepository);
        }

        @Test
        void throwsResourceNotFound_whenPickListDoesNotExist() {
            UUID missingId = UUID.randomUUID();
            when(pickListRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    packingService.createTask(missingId, assignedToId, null, createdById))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsResourceNotFound_whenStationDoesNotExist() {
            UUID missingStationId = UUID.randomUUID();
            when(pickListRepository.findById(pickList.getId())).thenReturn(Optional.of(pickList));
            when(packingStationRepository.findById(missingStationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    packingService.createTask(pickList.getId(), assignedToId, missingStationId, createdById))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void assignsStation_whenStationIdProvided() {
            PackingStation station = new PackingStation();
            station.setId(UUID.randomUUID());
            station.setCode("PACK-01");

            when(pickListRepository.findById(pickList.getId())).thenReturn(Optional.of(pickList));
            when(packingStationRepository.findById(station.getId())).thenReturn(Optional.of(station));
            when(packingTaskRepository.existsByTaskNumber(any())).thenReturn(false);
            when(packingTaskRepository.save(any(PackingTask.class))).thenAnswer(inv -> {
                PackingTask t = inv.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(assignedToId));

            PackingTaskResponse result = packingService.createTask(pickList.getId(), assignedToId, station.getId(), createdById);

            assertThat(result.stationCode()).isEqualTo("PACK-01");
        }

        @Test
        void throwsIllegalState_whenTaskNumberGenerationExhaustsAttempts() {
            when(pickListRepository.findById(pickList.getId())).thenReturn(Optional.of(pickList));
            when(packingTaskRepository.existsByTaskNumber(any())).thenReturn(true); // always collides

            assertThatThrownBy(() ->
                    packingService.createTask(pickList.getId(), assignedToId, null, createdById))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unique task number");

            verify(packingTaskRepository, never()).save(any());
        }
    }

    @Nested
    class StartTask {

        @Test
        void startsTask_whenPending() {
            when(packingTaskRepository.findById(task.getId())).thenReturn(Optional.of(task));
            when(packingTaskRepository.save(task)).thenAnswer(inv -> {
                PackingTask t = inv.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(assignedToId, startedById));

            PackingTaskResponse result = packingService.startTask(task.getId(), startedById);

            assertThat(result.status()).isEqualTo(PackingTaskStatus.IN_PROGRESS);
            assertThat(task.getStartedBy()).isEqualTo(startedById);
            assertThat(task.getStartedAt()).isNotNull();
            verify(eventPublisher).publish(any());
            verify(packingTaskEventRepository).save(any(PackingTaskEvent.class));
        }

        @Test
        void throwsIllegalArgument_whenTaskNotPending() {
            task.setStatus(PackingTaskStatus.IN_PROGRESS);
            when(packingTaskRepository.findById(task.getId())).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> packingService.startTask(task.getId(), startedById))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not in PENDING status");
        }

        @Test
        void throwsResourceNotFound_whenTaskDoesNotExist() {
            UUID missingId = UUID.randomUUID();
            when(packingTaskRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> packingService.startTask(missingId, startedById))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class CompleteTask {

        @BeforeEach
        void setUpInProgress() {
            task.setStatus(PackingTaskStatus.IN_PROGRESS);
        }

        @Test
        void completesTask_andCreatesParcel() {
            when(packingTaskRepository.findById(task.getId())).thenReturn(Optional.of(task));
            when(packingTaskRepository.save(task)).thenReturn(task);
            doAnswer(inv -> {
                Parcel p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return null;
            }).when(parcelRepository).save(any(Parcel.class));
            when(parcelRepository.existsByTrackingNumber(any())).thenReturn(false);
            when(parcelRepository.existsByBarcode(any())).thenReturn(false);
            when(orderLineRepository.findByOrder_Id(order.getId())).thenReturn(List.of(orderLine));

            PackingCompleteRequest request = new PackingCompleteRequest(BigDecimal.valueOf(2.5), Map.of());
            ParcelResponse result = packingService.completeTask(task.getId(), request, startedById);

            assertThat(result.status()).isEqualTo(PackageStatus.PACKED);
            assertThat(result.trackingNumber()).startsWith("TRK-");
            assertThat(result.barcode()).startsWith("BAR-");
            assertThat(task.getStatus()).isEqualTo(PackingTaskStatus.COMPLETED);
            assertThat(task.getCompletedAt()).isNotNull();
            assertThat(orderLine.getStatus()).isEqualTo(OrderLineStatus.PACKED);
            assertThat(orderLine.getQuantityPacked()).isEqualTo(orderLine.getQuantityPicked());
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PACKING);
            verify(parcelRepository).save(any(Parcel.class));
            verify(orderRepository).save(order);
        }

        @Test
        void throwsIllegalArgument_whenTaskNotInProgress() {
            task.setStatus(PackingTaskStatus.PENDING);
            when(packingTaskRepository.findById(task.getId())).thenReturn(Optional.of(task));

            PackingCompleteRequest request = new PackingCompleteRequest(BigDecimal.valueOf(2.5), Map.of());

            assertThatThrownBy(() -> packingService.completeTask(task.getId(), request, startedById))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not IN_PROGRESS");

            verify(parcelRepository, never()).save(any());
        }

        @Test
        void throwsResourceNotFound_whenTaskDoesNotExist() {
            UUID missingId = UUID.randomUUID();
            when(packingTaskRepository.findById(missingId)).thenReturn(Optional.empty());

            PackingCompleteRequest request = new PackingCompleteRequest(BigDecimal.valueOf(2.5), Map.of());

            assertThatThrownBy(() -> packingService.completeTask(missingId, request, startedById))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetAll {

        @Test
        void returnsEmptyList_whenNoTasksExist() {
            when(packingTaskRepository.findAll()).thenReturn(List.of());

            List<PackingTaskResponse> result = packingService.getAll(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(userService);
        }

        @Test
        void usesFindAll_whenStatusIsNull() {
            when(packingTaskRepository.findAll()).thenReturn(List.of(task));
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(assignedToId));

            List<PackingTaskResponse> result = packingService.getAll(null);

            assertThat(result).hasSize(1);
            verify(packingTaskRepository).findAll();
            verify(packingTaskRepository, never()).findByStatus(any());
        }

        @Test
        void usesFindByStatus_whenStatusProvided() {
            when(packingTaskRepository.findByStatus(PackingTaskStatus.COMPLETED)).thenReturn(List.of(task));
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(assignedToId));

            packingService.getAll(PackingTaskStatus.COMPLETED);

            verify(packingTaskRepository).findByStatus(PackingTaskStatus.COMPLETED);
            verify(packingTaskRepository, never()).findAll();
        }
    }

    @Nested
    class GetByWorker {

        @Test
        void returnsTasksAssignedToWorker() {
            when(packingTaskRepository.findByAssignedTo(assignedToId)).thenReturn(List.of(task));
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(assignedToId));

            List<PackingTaskResponse> result = packingService.getByWorker(assignedToId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).assignedTo().id()).isEqualTo(assignedToId);
        }

        @Test
        void returnsEmptyList_whenWorkerHasNoTasks() {
            when(packingTaskRepository.findByAssignedTo(assignedToId)).thenReturn(List.of());

            List<PackingTaskResponse> result = packingService.getByWorker(assignedToId);

            assertThat(result).isEmpty();
            verifyNoInteractions(userService);
        }
    }
}