package com.nexuswms.inventory.service;

import com.nexuswms.common.exception.InsufficientStockException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.inventory.dto.request.StockAdjustmentRequest;
import com.nexuswms.inventory.dto.response.SkuLocationResponse;
import com.nexuswms.inventory.entity.*;
import com.nexuswms.inventory.repository.*;
import com.nexuswms.user.entity.Role;
import com.nexuswms.user.entity.User;
import com.nexuswms.user.entity.UserStatus;
import com.nexuswms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockService — scoped to adjustStock() and the reorder
 * alert trigger/resolve logic reachable through it (both private, tested
 * indirectly via adjustStock()'s branches). findAvailableLocationForSku()
 * and adjustFromGoodsReceipt() are out of scope for this pass.
 *
 * Shelf's aisle/zone chain is mocked rather than built from real Aisle/Zone
 * entities — StockService only calls a couple of getters/setters on that
 * chain via updateZoneOccupancy(), so mocking avoids needing their full
 * entity source.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private SkuRepository skuRepository;
    @Mock private ShelfRepository shelfRepository;
    @Mock private SkuLocationRepository skuLocationRepository;
    @Mock private ReorderAlertRepository reorderAlertRepository;
    @Mock private UserRepository userRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private WarehouseEventPublisher eventPublisher;
    @Mock private ZoneRepository zoneRepository;

    private StockService stockService;

    private Sku sku;
    private Shelf shelf;
    private User worker;
    private Aisle aisle;
    private Zone zone;

    @BeforeEach
    void setUp() {
        stockService = new StockService(
                skuRepository, shelfRepository, skuLocationRepository, reorderAlertRepository,
                userRepository, stockMovementRepository, eventPublisher, zoneRepository);

        sku = Sku.builder()
                .id(UUID.randomUUID())
                .skuCode("SKU-001")
                .name("Test Widget")
                .reorderPoint(10)
                .reorderQuantity(50)
                .build();

        // Mocked aisle/zone chain — StockService only calls getAisle()/getZone()/
        // getCurrentOccupancy()/setCurrentOccupancy() on this path.
        zone = mock(Zone.class);
        lenient().when(zone.getCurrentOccupancy()).thenReturn(100);
        aisle = mock(Aisle.class);
        lenient().when(aisle.getZone()).thenReturn(zone);

        shelf = mock(Shelf.class);
        lenient().when(shelf.getId()).thenReturn(UUID.randomUUID());
        lenient().when(shelf.getCode()).thenReturn("A1-G01");
        lenient().when(shelf.getAisle()).thenReturn(aisle);

        worker = User.builder()
                .id(UUID.randomUUID())
                .employeeId("EMP-0001")
                .email("worker@nexuswms.com")
                .name("Worker One")
                .role(Role.INVENTORY_CONTROLLER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /** Builds a request against the fixture sku/shelf. */
    private StockAdjustmentRequest request(int quantity, String batchId) {
        return new StockAdjustmentRequest(sku.getId(), shelf.getId(), quantity, batchId, "test adjustment");
    }

    @Nested
    class AdjustStock {

        @Test
        void throwsIllegalArgument_whenQuantityIsZero() {
            assertThatThrownBy(() ->
                    stockService.adjustStock(request(0, "BATCH-1"), worker.getId().toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be zero");

            verifyNoInteractions(skuRepository);
        }

        @Test
        void throwsResourceNotFound_whenSkuDoesNotExist() {
            when(skuRepository.findById(sku.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    stockService.adjustStock(request(10, "BATCH-1"), worker.getId().toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsResourceNotFound_whenShelfDoesNotExist() {
            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    stockService.adjustStock(request(10, "BATCH-1"), worker.getId().toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsResourceNotFound_whenWorkerDoesNotExist() {
            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    stockService.adjustStock(request(10, "BATCH-1"), worker.getId().toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsInsufficientStock_whenAdjustmentWouldGoBelowZero() {
            SkuLocation existing = SkuLocation.builder()
                    .sku(sku).shelf(shelf).quantity(5).reservedQuantity(0).batchId("BATCH-1").build();

            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
            when(skuLocationRepository.findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), "BATCH-1"))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() ->
                    stockService.adjustStock(request(-10, "BATCH-1"), worker.getId().toString()))
                    .isInstanceOf(InsufficientStockException.class);

            verify(skuLocationRepository, never()).save(any());
        }

        @Test
        void throwsInsufficientStock_whenAdjustmentWouldGoBelowReservedQuantity() {
            SkuLocation existing = SkuLocation.builder()
                    .sku(sku).shelf(shelf).quantity(10).reservedQuantity(8).batchId("BATCH-1").build();

            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
            when(skuLocationRepository.findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), "BATCH-1"))
                    .thenReturn(Optional.of(existing));

            // -5 => newQuantity = 5, which is below reservedQuantity of 8
            assertThatThrownBy(() ->
                    stockService.adjustStock(request(-5, "BATCH-1"), worker.getId().toString()))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        void createsNewLocation_whenNoneExistsForSkuShelfBatch() {
            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
            when(skuLocationRepository.findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), "BATCH-1"))
                    .thenReturn(Optional.empty());
            when(skuLocationRepository.getTotalQuantityBySkuId(sku.getId())).thenReturn(50);

            SkuLocationResponse result = stockService.adjustStock(request(20, "BATCH-1"), worker.getId().toString());

            assertThat(result).isNotNull();
            verify(skuLocationRepository).save(argThat(loc -> loc.getQuantity() == 20));
            verify(stockMovementRepository).save(argThat(movement ->
                    movement.getMovementType().equals("ADJUSTMENT") && movement.getQuantity() == 20));
            verify(zone).setCurrentOccupancy(120); // 100 + 20
            verify(zoneRepository).save(zone);
        }

        @Test
        void triggersReorderAlert_whenDecreaseDropsStockToOrBelowReorderPoint_andNoOpenAlertExists() {
            SkuLocation existing = SkuLocation.builder()
                    .sku(sku).shelf(shelf).quantity(15).reservedQuantity(0).batchId("BATCH-1").build();

            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
            when(skuLocationRepository.findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), "BATCH-1"))
                    .thenReturn(Optional.of(existing));
            // reorderPoint is 10; total after -10 adjustment = 5, which is <= 10
            when(skuLocationRepository.getTotalQuantityBySkuId(sku.getId())).thenReturn(5);
            when(reorderAlertRepository.existsBySkuIdAndStatus(sku.getId(), "OPEN")).thenReturn(false);

            stockService.adjustStock(request(-10, "BATCH-1"), worker.getId().toString());

            verify(reorderAlertRepository).save(argThat(alert ->
                    alert.getStatus().equals("OPEN") && alert.getCurrentQuantity() == 5));
            verify(eventPublisher).publishAlert(any());
        }

        @Test
        void doesNotDuplicateReorderAlert_whenOpenAlertAlreadyExists() {
            SkuLocation existing = SkuLocation.builder()
                    .sku(sku).shelf(shelf).quantity(15).reservedQuantity(0).batchId("BATCH-1").build();

            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
            when(skuLocationRepository.findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), "BATCH-1"))
                    .thenReturn(Optional.of(existing));
            when(skuLocationRepository.getTotalQuantityBySkuId(sku.getId())).thenReturn(5);
            when(reorderAlertRepository.existsBySkuIdAndStatus(sku.getId(), "OPEN")).thenReturn(true);

            stockService.adjustStock(request(-10, "BATCH-1"), worker.getId().toString());

            verify(reorderAlertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void resolvesOpenReorderAlert_whenIncreaseBringsStockAboveReorderPoint() {
            SkuLocation existing = SkuLocation.builder()
                    .sku(sku).shelf(shelf).quantity(5).reservedQuantity(0).batchId("BATCH-1").build();
            ReorderAlert openAlert = ReorderAlert.builder()
                    .id(UUID.randomUUID()).sku(sku).currentQuantity(5).reorderPoint(10).status("OPEN").build();

            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
            when(skuLocationRepository.findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), "BATCH-1"))
                    .thenReturn(Optional.of(existing));
            // reorderPoint is 10; total after +20 adjustment = 25, which is > 10
            when(skuLocationRepository.getTotalQuantityBySkuId(sku.getId())).thenReturn(25);
            when(reorderAlertRepository.findBySkuIdAndStatus(sku.getId(), "OPEN")).thenReturn(Optional.of(openAlert));

            stockService.adjustStock(request(20, "BATCH-1"), worker.getId().toString());

            assertThat(openAlert.getStatus()).isEqualTo("RESOLVED");
            verify(reorderAlertRepository).save(openAlert);
            verify(eventPublisher).publishAlert(any());
        }

        @Test
        void doesNotResolveAlert_whenNoOpenAlertExists() {
            SkuLocation existing = SkuLocation.builder()
                    .sku(sku).shelf(shelf).quantity(5).reservedQuantity(0).batchId("BATCH-1").build();

            when(skuRepository.findById(sku.getId())).thenReturn(Optional.of(sku));
            when(shelfRepository.findById(shelf.getId())).thenReturn(Optional.of(shelf));
            when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
            when(skuLocationRepository.findBySkuIdAndShelfIdAndBatchId(sku.getId(), shelf.getId(), "BATCH-1"))
                    .thenReturn(Optional.of(existing));
            when(skuLocationRepository.getTotalQuantityBySkuId(sku.getId())).thenReturn(25);
            when(reorderAlertRepository.findBySkuIdAndStatus(sku.getId(), "OPEN")).thenReturn(Optional.empty());

            stockService.adjustStock(request(20, "BATCH-1"), worker.getId().toString());

            verify(reorderAlertRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }
}