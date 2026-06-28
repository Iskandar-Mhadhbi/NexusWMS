package com.nexuswms.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shelves")
@Getter
@Setter
@ToString(exclude = {"aisle"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shelf {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id", nullable = false)
    private Aisle aisle;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String level;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "max_weight", precision = 10, scale = 2)
    private BigDecimal maxWeight;

    @PositiveOrZero
    @Column(name = "current_weight", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentWeight = BigDecimal.ZERO;
}