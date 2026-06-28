// CarrierRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarrierRepository extends JpaRepository<Carrier, UUID> {
    List<Carrier> findByIsActiveTrue();
    Optional<Carrier> findByCode(String code);
    boolean existsByCode(String code);
}