package com.nexuswms.procurement.repository;

import com.nexuswms.procurement.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    boolean existsByCode(String code);

    Optional<Supplier> findByCode(String code);
}