package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.fulfillment.dto.request.PickItemRequest;
import com.nexuswms.fulfillment.dto.response.PickListItemResponse;
import com.nexuswms.fulfillment.dto.response.PickListResponse;
import com.nexuswms.fulfillment.entity.*;
import com.nexuswms.fulfillment.repository.*;
import com.nexuswms.inventory.service.StockService;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PickListService — scoped to getAll(), getByWorker(),
 * getById(), and pickItem(). generate() is deliberately excluded (larger
 * dependency surface, tested separately if/when needed).
 */
@ExtendWith(MockitoExtension.class)
class PickListServiceTest {

    @Mock private PickListRepository pickListRepository;
    @Mock private PickListItemRepository pickListItemRepository;
    @Mock private FulfillmentRequestRepository fulfillmentRequestRepository;
    @Mock private OrderLineRepository orderLineRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private StockService stockService;
    @Mock private WarehouseEventPublisher eventPublisher;
    @Mock private UserService userService;

    private PickListService pickListService;

    private UUID generatedById;
    private UUID assignedToId;
    private PickList pickList;
    private PickListItem item;
    private FulfillmentRequest fulfillmentRequest;
    private OrderLine orderLine;

    @BeforeEach
    void setUp() {
        pickListService = new PickListService(
                pickListRepository, pickListItemRepository, fulfillmentRequestRepository,
                orderLineRepository, orderRepository, stockService, eventPublisher, userService);

        generatedById = UUID.randomUUID();
        assignedToId = UUID.randomUUID();

        fulfillmentRequest = new FulfillmentRequest();
        fulfillmentRequest.setId(UUID.randomUUID());

        pickList = new PickList();
        pickList.setId(UUID.randomUUID());
        pickList.setFulfillmentRequest(fulfillmentRequest);
        pickList.setGeneratedBy(generatedById);
        pickList.setAssignedTo(assignedToId);
        pickList.setStatus(PickListStatus.GENERATED);
        pickList.setGeneratedAt(LocalDateTime.now());

        orderLine = new OrderLine();
        orderLine.setId(UUID.randomUUID());
        orderLine.setSkuId(UUID.randomUUID());
        orderLine.setQuantityOrdered(5);
        orderLine.setQuantityPicked(0);
        orderLine.setStatus(OrderLineStatus.PICKING);

        item = new PickListItem();
        item.setId(UUID.randomUUID());
        item.setPickList(pickList);
        item.setOrderLine(orderLine);
        item.setSkuId(UUID.randomUUID());
        item.setShelfId(UUID.randomUUID());
        item.setShelfCode("A1-G01");
        item.setQuantityToPick(5);
        item.setQuantityPicked(0);
        item.setStatus(PickListItemStatus.PENDING);
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
    class GetById {

        @Test
        void returnsEnrichedResponse_whenPickListExists() {
            when(pickListRepository.findById(pickList.getId())).thenReturn(Optional.of(pickList));
            when(pickListItemRepository.findByPickList_Id(pickList.getId())).thenReturn(List.of(item));
            when(userService.getUserSummaries(Set.of(generatedById, assignedToId)))
                    .thenReturn(userSummaryMapFor(generatedById, assignedToId));

            PickListResponse result = pickListService.getById(pickList.getId());

            assertThat(result.id()).isEqualTo(pickList.getId());
            assertThat(result.generatedBy().id()).isEqualTo(generatedById);
            assertThat(result.assignedTo().id()).isEqualTo(assignedToId);
            assertThat(result.items()).hasSize(1);
        }

        @Test
        void throwsResourceNotFound_whenPickListDoesNotExist() {
            UUID missingId = UUID.randomUUID();
            when(pickListRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pickListService.getById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetAll {

        @Test
        void returnsEmptyList_whenNoPickListsExist() {
            when(pickListRepository.findAll()).thenReturn(List.of());

            List<PickListResponse> result = pickListService.getAll(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(userService);
        }

        @Test
        void usesFindAll_whenStatusIsNull() {
            when(pickListRepository.findAll()).thenReturn(List.of(pickList));
            when(pickListItemRepository.findByPickList_IdIn(List.of(pickList.getId()))).thenReturn(List.of(item));
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(generatedById, assignedToId));

            List<PickListResponse> result = pickListService.getAll(null);

            assertThat(result).hasSize(1);
            verify(pickListRepository).findAll();
            verify(pickListRepository, never()).findByStatus(any());
        }

        @Test
        void usesFindByStatus_whenStatusProvided() {
            when(pickListRepository.findByStatus(PickListStatus.COMPLETED)).thenReturn(List.of(pickList));
            when(pickListItemRepository.findByPickList_IdIn(List.of(pickList.getId()))).thenReturn(List.of(item));
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(generatedById, assignedToId));

            pickListService.getAll(PickListStatus.COMPLETED);

            verify(pickListRepository).findByStatus(PickListStatus.COMPLETED);
            verify(pickListRepository, never()).findAll();
        }
    }

    @Nested
    class GetByWorker {

        @Test
        void returnsPickListsAssignedToWorker() {
            when(pickListRepository.findByAssignedTo(assignedToId)).thenReturn(List.of(pickList));
            when(pickListItemRepository.findByPickList_IdIn(List.of(pickList.getId()))).thenReturn(List.of(item));
            when(userService.getUserSummaries(any())).thenReturn(userSummaryMapFor(generatedById, assignedToId));

            List<PickListResponse> result = pickListService.getByWorker(assignedToId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).assignedTo().id()).isEqualTo(assignedToId);
        }

        @Test
        void returnsEmptyList_whenWorkerHasNoPickLists() {
            when(pickListRepository.findByAssignedTo(assignedToId)).thenReturn(List.of());

            List<PickListResponse> result = pickListService.getByWorker(assignedToId);

            assertThat(result).isEmpty();
            verifyNoInteractions(userService);
        }
    }

    @Nested
    class PickItem {

        @Test
        void picksItem_whenValid() {
            when(pickListItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(pickListItemRepository.save(item)).thenReturn(item);
            when(pickListItemRepository.findByPickList_Id(pickList.getId())).thenReturn(List.of(item));

            PickItemRequest request = new PickItemRequest(5);
            PickListItemResponse result = pickListService.pickItem(pickList.getId(), item.getId(), request, assignedToId);

            assertThat(result.status()).isEqualTo(PickListItemStatus.PICKED);
            assertThat(orderLine.getQuantityPicked()).isEqualTo(5);
            assertThat(orderLine.getStatus()).isEqualTo(OrderLineStatus.PICKED);
            verify(orderLineRepository).save(orderLine);
            verify(eventPublisher).publish(any());
        }

        @Test
        void completesPickList_whenLastItemPicked() {
            when(pickListItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(pickListItemRepository.save(item)).thenReturn(item);
            // After picking, findByPickList_Id is called again to check completion — item is now PICKED by this point
            when(pickListItemRepository.findByPickList_Id(pickList.getId())).thenReturn(List.of(item));

            pickListService.pickItem(pickList.getId(), item.getId(), new PickItemRequest(5), assignedToId);

            assertThat(pickList.getStatus()).isEqualTo(PickListStatus.COMPLETED);
            verify(pickListRepository).save(pickList);
        }

        @Test
        void doesNotCompletePickList_whenOtherItemsStillPending() {
            PickListItem secondItem = new PickListItem();
            secondItem.setId(UUID.randomUUID());
            secondItem.setPickList(pickList);
            secondItem.setStatus(PickListItemStatus.PENDING);

            when(pickListItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(pickListItemRepository.save(item)).thenReturn(item);
            when(pickListItemRepository.findByPickList_Id(pickList.getId())).thenReturn(List.of(item, secondItem));

            pickListService.pickItem(pickList.getId(), item.getId(), new PickItemRequest(5), assignedToId);

            assertThat(pickList.getStatus()).isEqualTo(PickListStatus.GENERATED);
            verify(pickListRepository, never()).save(any());
        }

        @Test
        void throwsResourceNotFound_whenItemDoesNotExist() {
            UUID missingId = UUID.randomUUID();
            when(pickListItemRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    pickListService.pickItem(pickList.getId(), missingId, new PickItemRequest(1), assignedToId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsResourceNotFound_whenItemBelongsToDifferentPickList() {
            UUID otherPickListId = UUID.randomUUID();
            when(pickListItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

            assertThatThrownBy(() ->
                    pickListService.pickItem(otherPickListId, item.getId(), new PickItemRequest(1), assignedToId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsIllegalArgument_whenPrincipalIsNotAssignedWorker() {
            UUID someoneElse = UUID.randomUUID();
            when(pickListItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

            assertThatThrownBy(() ->
                    pickListService.pickItem(pickList.getId(), item.getId(), new PickItemRequest(1), someoneElse))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not assigned");
        }

        @Test
        void throwsIllegalArgument_whenItemAlreadyPicked() {
            item.setStatus(PickListItemStatus.PICKED);
            when(pickListItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

            assertThatThrownBy(() ->
                    pickListService.pickItem(pickList.getId(), item.getId(), new PickItemRequest(1), assignedToId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already picked");
        }

        @Test
        void throwsIllegalArgument_whenQuantityExceedsQuantityToPick() {
            when(pickListItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

            assertThatThrownBy(() ->
                    pickListService.pickItem(pickList.getId(), item.getId(), new PickItemRequest(999), assignedToId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds");
        }
    }
}