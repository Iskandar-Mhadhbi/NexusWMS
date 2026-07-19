package com.nexuswms.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sku_locations")
@Getter
@Setter
@ToString(exclude = {"shelf","sku"})  
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id", nullable = false)
    private Shelf shelf;

    @PositiveOrZero
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @PositiveOrZero
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "batch_id", length = 100)
    private String batchId;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}