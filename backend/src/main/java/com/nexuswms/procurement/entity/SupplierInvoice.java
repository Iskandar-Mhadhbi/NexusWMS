package com.nexuswms.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier_invoices")
@Getter
@Setter
@ToString(exclude = {"purchaseOrder"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    // Denormalized for faster invoice queries — avoids join through PO every time
    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private BigDecimal invoiceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ThreeWayMatchStatus threeWayMatchStatus = ThreeWayMatchStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}