package com.nexuswms.procurement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goods_receipt_lines")
@Getter
@Setter
@ToString(exclude = {"goodsReceipt"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    // UUID references — cross-package boundaries
    @Column(name = "po_line_id", nullable = false)
    private UUID poLineId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private Integer quantityReceived;

    private String batchId;

    private LocalDate expiryDate;

    @Column(name = "shelf_id")
    private UUID shelfId;
}