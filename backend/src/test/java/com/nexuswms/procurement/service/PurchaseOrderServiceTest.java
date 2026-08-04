package com.nexuswms.procurement.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.inventory.entity.Sku;
import com.nexuswms.inventory.service.SkuService;
import com.nexuswms.procurement.dto.request.PurchaseOrderLineRequest;
import com.nexuswms.procurement.dto.request.PurchaseOrderRequest;
import com.nexuswms.procurement.dto.response.PurchaseOrderResponse;
import com.nexuswms.procurement.entity.*;
import com.nexuswms.procurement.repository.*;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Unit tests for PurchaseOrderService — scoped to create() and approve().
 * getAll()/getById() are thin read wrappers, out of scope for this pass.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository poRepository;
    @Mock private PurchaseOrderLineRepository poLineRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private SkuService skuService;
    @Mock private UserService userService;

    private PurchaseOrderService purchaseOrderService;

    private UUID requestedById;
    private UUID approvedById;
    private Supplier supplier;
    private Sku sku;

    @BeforeEach
    void setUp() {
        purchaseOrderService = new PurchaseOrderService(
                poRepository, poLineRepository, supplierRepository, skuService, userService);

        requestedById = UUID.randomUUID();
        approvedById = UUID.randomUUID();

        supplier = Supplier.builder()
                .id(UUID.randomUUID())
                .name("TechParts Global")
                .code("TPG")
                .status(SupplierStatus.ACTIVE)
                .paymentTerms(30)
                .createdAt(LocalDateTime.now())
                .build();

        sku = Sku.builder()
                .id(UUID.randomUUID())
                .skuCode("SKU-001")
                .name("Samsung Galaxy S24")
                .reorderPoint(10)
                .reorderQuantity(50)
                .build();
    }

    /** Builds a stub UserSummaryResponse map for the given IDs. */
    private Map<UUID, UserSummaryResponse> userSummaryMapFor(UUID... ids) {
        Map<UUID, UserSummaryResponse> map = new HashMap<>();
        for (UUID id : ids) {
            map.put(id, new UserSummaryResponse(id, "EMP-" + id.toString().substring(0, 4), "Test", "test@x.com", null, null));
        }
        return map;
    }

    @Nested
    class Create {

        @Test
        void createsPo_whenSupplierActiveAndSkusExist() {
            PurchaseOrderLineRequest lineReq = new PurchaseOrderLineRequest(sku.getId(), 10, BigDecimal.valueOf(100));
            PurchaseOrderRequest request = new PurchaseOrderRequest(
                    supplier.getId(), LocalDate.now().plusDays(7), List.of(lineReq));

            when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
            when(skuService.getSkuMapByIds(Set.of(sku.getId()))).thenReturn(Map.of(sku.getId(), sku));
            when(poRepository.existsByPoNumber(any())).thenReturn(false);
            when(poRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
                PurchaseOrder po = inv.getArgument(0);
                po.setId(UUID.randomUUID());
                return po;
            });
            when(userService.getUserSummaries(Set.of(requestedById)))
                    .thenReturn(userSummaryMapFor(requestedById));

            PurchaseOrderResponse result = purchaseOrderService.create(request, requestedById);

            assertThat(result.poNumber()).startsWith("PO-");
            assertThat(result.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(result.requestedBy().id()).isEqualTo(requestedById);
            assertThat(result.approvedBy()).isNull();
            assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(result.lines()).hasSize(1);
            verify(poLineRepository).saveAll(any());
        }

        @Test
        void throwsResourceNotFound_whenSupplierDoesNotExist() {
            PurchaseOrderRequest request = new PurchaseOrderRequest(
                    supplier.getId(), LocalDate.now(), List.of());
            when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseOrderService.create(request, requestedById))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsIllegalArgument_whenSupplierNotActive() {
            supplier.setStatus(SupplierStatus.BLACKLISTED);
            PurchaseOrderRequest request = new PurchaseOrderRequest(
                    supplier.getId(), LocalDate.now(), List.of());
            when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));

            assertThatThrownBy(() -> purchaseOrderService.create(request, requestedById))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not ACTIVE");

            verifyNoInteractions(skuService);
        }

        @Test
        void throwsResourceNotFound_whenSkuDoesNotExist() {
            PurchaseOrderLineRequest lineReq = new PurchaseOrderLineRequest(sku.getId(), 10, BigDecimal.valueOf(100));
            PurchaseOrderRequest request = new PurchaseOrderRequest(
                    supplier.getId(), LocalDate.now(), List.of(lineReq));

            when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
            when(skuService.getSkuMapByIds(Set.of(sku.getId()))).thenReturn(Map.of()); // SKU not found

            assertThatThrownBy(() -> purchaseOrderService.create(request, requestedById))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(poRepository, never()).save(any());
        }

        @Test
        void throwsIllegalState_whenPoNumberGenerationExhaustsAttempts() {
            PurchaseOrderRequest request = new PurchaseOrderRequest(
                    supplier.getId(), LocalDate.now(), List.of());

            when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
            when(skuService.getSkuMapByIds(any())).thenReturn(Map.of());
            when(poRepository.existsByPoNumber(any())).thenReturn(true); // always collides

            assertThatThrownBy(() -> purchaseOrderService.create(request, requestedById))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unique PO number");

            verify(poRepository, never()).save(any());
        }
    }

    @Nested
    class Approve {

        private PurchaseOrder draftPo;

        @BeforeEach
        void setUpDraftPo() {
            draftPo = PurchaseOrder.builder()
                    .id(UUID.randomUUID())
                    .poNumber("PO-20260803-00001")
                    .supplier(supplier)
                    .requestedBy(requestedById)
                    .status(PurchaseOrderStatus.DRAFT)
                    .totalAmount(BigDecimal.valueOf(1000))
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        @Test
        void approvesPo_whenStatusIsDraft() {
            when(poRepository.findById(draftPo.getId())).thenReturn(Optional.of(draftPo));
            when(poRepository.save(draftPo)).thenReturn(draftPo);
            when(poLineRepository.findByPurchaseOrder_Id(draftPo.getId())).thenReturn(List.of());
            when(skuService.getSkuMapByIds(any())).thenReturn(Map.of());
            when(userService.getUserSummaries(any()))
                    .thenReturn(userSummaryMapFor(requestedById, approvedById));

            PurchaseOrderResponse result = purchaseOrderService.approve(draftPo.getId(), approvedById);

            assertThat(result.status()).isEqualTo(PurchaseOrderStatus.APPROVED);
            assertThat(result.approvedBy().id()).isEqualTo(approvedById);
            assertThat(draftPo.getApprovedBy()).isEqualTo(approvedById);
        }

        @Test
        void approvesPo_whenStatusIsPendingApproval() {
            draftPo.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
            when(poRepository.findById(draftPo.getId())).thenReturn(Optional.of(draftPo));
            when(poRepository.save(draftPo)).thenReturn(draftPo);
            when(poLineRepository.findByPurchaseOrder_Id(draftPo.getId())).thenReturn(List.of());
            when(skuService.getSkuMapByIds(any())).thenReturn(Map.of());
            when(userService.getUserSummaries(any()))
                    .thenReturn(userSummaryMapFor(requestedById, approvedById));

            PurchaseOrderResponse result = purchaseOrderService.approve(draftPo.getId(), approvedById);

            assertThat(result.status()).isEqualTo(PurchaseOrderStatus.APPROVED);
        }

        @Test
        void throwsIllegalArgument_whenAlreadyApproved() {
            draftPo.setStatus(PurchaseOrderStatus.APPROVED);
            when(poRepository.findById(draftPo.getId())).thenReturn(Optional.of(draftPo));

            assertThatThrownBy(() -> purchaseOrderService.approve(draftPo.getId(), approvedById))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DRAFT or PENDING_APPROVAL");

            verify(poRepository, never()).save(any());
        }

        @Test
        void throwsResourceNotFound_whenPoDoesNotExist() {
            UUID missingId = UUID.randomUUID();
            when(poRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseOrderService.approve(missingId, approvedById))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}