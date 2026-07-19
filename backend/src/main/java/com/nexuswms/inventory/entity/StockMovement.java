package com.nexuswms.inventory.entity;

import com.nexuswms.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@ToString(exclude = {"sku","shelf","worker"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private Shelf shelf;

    @NotNull
    @Column(nullable = false)
    private Integer quantity;

    @NotBlank
    @Column(name = "movement_type", nullable = false, length = 30)
    private String movementType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    @Column(name = "batch_id", length = 100)
    private String batchId;

    @Column(name = "moved_at", nullable = false, updatable = false)
    private LocalDateTime movedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        movedAt = LocalDateTime.now();
    }
}