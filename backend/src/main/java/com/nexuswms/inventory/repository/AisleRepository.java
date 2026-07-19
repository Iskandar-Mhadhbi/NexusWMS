package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.Aisle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AisleRepository extends JpaRepository<Aisle, UUID> {
    List<Aisle> findByZoneId(UUID zoneId);
    Optional<Aisle> findByCode(String code);
    boolean existsByZoneIdAndCode(UUID zoneId, String code);
    boolean existsByCode(String code);
}