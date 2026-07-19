// DispatchService.java
package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.dashboard.WarehouseEvent;
import com.nexuswms.dashboard.WarehouseEventPublisher;
import com.nexuswms.fulfillment.dto.request.DispatchRequest;
import com.nexuswms.fulfillment.dto.response.ShipmentResponse;
import com.nexuswms.fulfillment.entity.*;
import com.nexuswms.fulfillment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final ParcelRepository parcelRepository;
    private final CarrierRepository carrierRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final FulfillmentRequestRepository fulfillmentRequestRepository;
    private final WarehouseEventPublisher eventPublisher;

    /* ----- Dispatch Parcel ----- */
    /**
     * Dispatches a parcel by assigning a carrier and creating a shipment record.
     * Transitions the parcel to DISPATCHED, the order to DISPATCHED,
     * and the fulfillment request to COMPLETED.
     * A carrier tracking number is auto-generated if not provided by the carrier.
     *
     * @param parcelId   the UUID of the parcel to dispatch
     * @param dispatchedBy the UUID of the dispatcher (from JWT principal)
     * @param request    the dispatch payload (carrierId, estimatedDelivery)
     * @return the created shipment as a response DTO
     * @throws ResourceNotFoundException if the parcel or carrier does not exist
     * @throws IllegalArgumentException  if the parcel is not in PACKED status
     */
    @Transactional
    public ShipmentResponse dispatch(UUID parcelId, UUID dispatchedBy, DispatchRequest request) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel not found with id: " + parcelId));

        if (parcel.getStatus() != PackageStatus.PACKED) {
            throw new IllegalArgumentException(
                    "Parcel is not in PACKED status. Current status: " + parcel.getStatus());
        }

        Carrier carrier = carrierRepository.findById(request.carrierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carrier not found with id: " + request.carrierId()));

        if (!carrier.getIsActive()) {
            throw new IllegalArgumentException("Carrier is not active: " + carrier.getName());
        }

        parcel.setStatus(PackageStatus.DISPATCHED);
        parcelRepository.save(parcel);

        Shipment shipment = new Shipment();
        shipment.setParcel(parcel);
        shipment.setCarrier(carrier);
        shipment.setCarrierTrackingNumber(generateCarrierTracking(carrier.getCode()));
        shipment.setDispatchedBy(dispatchedBy);
        shipment.setEstimatedDelivery(request.estimatedDelivery());
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment=shipmentRepository.save(shipment);

        Order order = parcel.getOrder();
        order.setStatus(OrderStatus.DISPATCHED);
        orderRepository.save(order);

        fulfillmentRequestRepository.findByOrder_Id(order.getId()).ifPresent(fr -> {
            fr.setStatus(FulfillmentStatus.COMPLETED);
            fulfillmentRequestRepository.save(fr);
        });

        eventPublisher.publish(WarehouseEvent.of(
            "ORDER_DISPATCHED",
            parcel.getId().toString(),
            parcel.getStatus().name(),
            dispatchedBy.toString(),
            "DISPATCH"
        ));

        return ShipmentResponse.from(shipment);
    }

    /* ----- Get Shipment By Parcel ----- */
    /**
     * Returns the shipment associated with a given parcel.
     *
     * @param parcelId the UUID of the parcel
     * @return the shipment as a response DTO
     * @throws ResourceNotFoundException if no shipment exists for the parcel
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getByParcel(UUID parcelId) {
        Shipment shipment = shipmentRepository.findByParcel_Id(parcelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found for parcel: " + parcelId));
        return ShipmentResponse.from(shipment);
    }

    /* ----- Get All Shipments ----- */
    /**
     * Returns all shipments, optionally filtered by status.
     * Manager-facing oversight query — distinct from getByParcel(), which
     * scopes to a single parcel's shipment.
     *
     * @param status optional status filter; if null, returns all shipments.
     * @return list of shipments matching the filter, as response DTOs
     */
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAll(ShipmentStatus status) {
        List<Shipment> shipments = (status != null)
                ? shipmentRepository.findByStatus(status)
                : shipmentRepository.findAll();

        return shipments.stream()
                .map(ShipmentResponse::from)
                .toList();
    }

    /* ----- Get Shipment By ID ----- */
    /**
     * Returns a single shipment by its own UUID.
     *
     * @param id the UUID of the shipment
     * @return the shipment as a response DTO
     * @throws ResourceNotFoundException if no shipment exists with the given id
     */
    @Transactional(readOnly = true)
    public ShipmentResponse getById(UUID id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + id));
        return ShipmentResponse.from(shipment);
    }
    
    /* ----- Private Helpers ----- */

    /**
     * Generates a carrier-prefixed tracking number in the format {CARRIER_CODE}-XXXXXXXXXX.
     */
    private String generateCarrierTracking(String carrierCode) {
        String random = String.format("%010d", new java.util.Random().nextLong(10_000_000_000L));
        return carrierCode.toUpperCase() + "-" + random;
    }
}