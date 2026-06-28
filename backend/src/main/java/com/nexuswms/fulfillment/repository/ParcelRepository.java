// ParcelRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParcelRepository extends JpaRepository<Parcel, UUID> {
    List<Parcel> findByOrder_Id(UUID orderId);
    Optional<Parcel> findByTrackingNumber(String trackingNumber);
    boolean existsByTrackingNumber(String trackingNumber);
    boolean existsByBarcode(String barcode);
}