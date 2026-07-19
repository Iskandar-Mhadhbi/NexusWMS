package com.nexuswms.fulfillment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pick_lists")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"fulfillmentRequest", "items"})
public class PickList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfillment_request_id", nullable = false)
    private FulfillmentRequest fulfillmentRequest;
    /* Cross-package: user context — UUID only */
    @Column(name = "generated_by", nullable = false, updatable = false)
    private UUID generatedBy;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;
    

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickListStatus status = PickListStatus.GENERATED;

    @OneToMany(mappedBy = "pickList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PickListItem> items = new ArrayList<>();

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}