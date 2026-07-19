package com.nexuswms.fulfillment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fulfillment_requests")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"order"})
public class FulfillmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "generated_by", nullable = false, updatable = false)
    private UUID generatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "assigned_zone")
    private String assignedZone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentStatus status = FulfillmentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}