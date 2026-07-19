package com.nexuswms.fulfillment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pick_list_items")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"pickList"})
public class PickListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pick_list_id", nullable = false)
    private PickList pickList;

    /* Same package but no cascade needed — OrderLine managed by Order */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id", nullable = false)
    private OrderLine orderLine;

    /* Cross-package: inventory context — UUID only */
    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "shelf_id", nullable = false)
    private UUID shelfId;

    @Column(name = "shelf_code")
    private String shelfCode;

    @Column(name = "quantity_to_pick", nullable = false)
    private Integer quantityToPick;

    @Column(name = "quantity_picked", nullable = false)
    private Integer quantityPicked = 0;

    @Column(name = "batch_id")
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickListItemStatus status = PickListItemStatus.PENDING;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;
}