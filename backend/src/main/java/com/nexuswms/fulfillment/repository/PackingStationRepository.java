// PackingStationRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.PackingStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackingStationRepository extends JpaRepository<PackingStation, UUID> {
    List<PackingStation> findByIsActiveTrue();
    boolean existsByCode(String code);
}