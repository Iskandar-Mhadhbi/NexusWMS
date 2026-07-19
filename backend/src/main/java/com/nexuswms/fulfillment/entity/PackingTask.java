package com.nexuswms.fulfillment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "packing_tasks")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"pickList", "station"})
public class PackingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pick_list_id", nullable = false)
    private PickList pickList;

    /* Cross-package: user context — UUID only */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @Column(name = "started_by")
    private UUID startedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private PackingStation station;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackingTaskStatus status = PackingTaskStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}