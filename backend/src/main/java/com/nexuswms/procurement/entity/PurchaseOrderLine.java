package com.nexuswms.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_lines")
@Getter
@Setter
@ToString(exclude = {"purchaseOrder"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    // UUID reference — Sku is in the inventory package, no cross-package @ManyToOne
    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private Integer quantityOrdered;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantityReceived = 0;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PurchaseOrderLineStatus status = PurchaseOrderLineStatus.PENDING;
}