package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShelfRepository extends JpaRepository<Shelf, UUID> {
    List<Shelf> findByAisleId(UUID aisleId);
    List<Shelf> findByAisleZoneId(UUID zoneId);
    Optional<Shelf> findByCode(String code);
    boolean existsByAisleIdAndCode(UUID aisleId, String code);
    boolean existsByCode(String code);
}