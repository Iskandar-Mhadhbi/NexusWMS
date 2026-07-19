package com.nexuswms.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@ToString(exclude = {"purchaseOrder"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "gr_number", nullable = false, unique = true)
    private String grNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) receivedAt = LocalDateTime.now();
    }
}