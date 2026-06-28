// CarrierService.java
package com.nexuswms.fulfillment.service;

import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.fulfillment.dto.request.CarrierRequest;
import com.nexuswms.fulfillment.dto.response.CarrierResponse;
import com.nexuswms.fulfillment.entity.Carrier;
import com.nexuswms.fulfillment.repository.CarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarrierService {

    private final CarrierRepository carrierRepository;

    /* ----- Create Carrier ----- */
    /**
     * Creates a new carrier. Carrier code must be unique across the system.
     *
     * @param request the carrier creation payload
     * @return the persisted carrier as a response DTO
     * @throws ConflictException if a carrier with the same code already exists
     */
    @Transactional
    public CarrierResponse create(CarrierRequest request) {
        if (carrierRepository.existsByCode(request.code().toUpperCase())) {
            throw new ConflictException("Carrier already exists with code: " + request.code());
        }

        Carrier carrier = new Carrier();
        carrier.setName(request.name());
        carrier.setCode(request.code().toUpperCase());
        carrier.setContactInfo(request.contactInfo());
        carrier.setIsActive(true);
        carrierRepository.save(carrier);

        return CarrierResponse.from(carrier);
    }

    /* ----- Get All Active Carriers ----- */
    /**
     * Returns all active carriers available for dispatch assignment.
     *
     * @return list of active carriers as response DTOs
     */
    @Transactional(readOnly = true)
    public List<CarrierResponse> getAllActive() {
        return carrierRepository.findByIsActiveTrue().stream()
                .map(CarrierResponse::from)
                .collect(Collectors.toList());
    }

    /* ----- Get All Carriers ----- */
    /**
     * Returns all carriers regardless of active status. Intended for admin use.
     *
     * @return list of all carriers as response DTOs
     */
    @Transactional(readOnly = true)
    public List<CarrierResponse> getAll() {
        return carrierRepository.findAll().stream()
                .map(CarrierResponse::from)
                .collect(Collectors.toList());
    }

    /* ----- Get Carrier By ID ----- */
    /**
     * Returns a single carrier by its UUID.
     *
     * @param id the carrier UUID
     * @return the carrier as a response DTO
     * @throws ResourceNotFoundException if no carrier exists with the given ID
     */
    @Transactional(readOnly = true)
    public CarrierResponse getById(UUID id) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier not found with id: " + id));
        return CarrierResponse.from(carrier);
    }
}