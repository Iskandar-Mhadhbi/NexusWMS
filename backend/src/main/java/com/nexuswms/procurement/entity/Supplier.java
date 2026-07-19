package com.nexuswms.procurement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
@Getter
@Setter 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> contactInfo;

    @Column(nullable = false)
    @Builder.Default
    private Integer paymentTerms = 30;

    private BigDecimal rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}