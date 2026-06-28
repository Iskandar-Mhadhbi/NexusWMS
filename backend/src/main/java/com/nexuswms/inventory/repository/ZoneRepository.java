package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
    Optional<Zone> findByName(String name);
    boolean existsByName(String name);
}