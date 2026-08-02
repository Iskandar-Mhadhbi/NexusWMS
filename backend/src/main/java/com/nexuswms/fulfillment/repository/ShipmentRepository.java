// ShipmentRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.Shipment;
import com.nexuswms.fulfillment.entity.ShipmentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByParcel_Id(UUID parcelId);
    List<Shipment> findByCarrier_Id(UUID carrierId);
    List<Shipment> findByStatus(ShipmentStatus status);
    boolean existsByCarrierTrackingNumber(String carrierTrackingNumber);
}