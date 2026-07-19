package com.nexuswms.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reorder_alerts")
@Getter
@Setter
@ToString(exclude = {"sku"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @PositiveOrZero
    @NotNull
    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @PositiveOrZero
    @NotNull
    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}