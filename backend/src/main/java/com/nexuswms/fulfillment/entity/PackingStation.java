package com.nexuswms.fulfillment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(name = "packing_stations")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PackingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /* Cross-package: inventory context — UUID only */
    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}