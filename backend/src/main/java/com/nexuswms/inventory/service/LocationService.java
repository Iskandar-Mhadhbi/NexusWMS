package com.nexuswms.inventory.service;
 
import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.inventory.dto.request.AisleRequest;
import com.nexuswms.inventory.dto.request.ShelfRequest;
import com.nexuswms.inventory.dto.request.ZoneRequest;
import com.nexuswms.inventory.dto.response.AisleResponse;
import com.nexuswms.inventory.dto.response.ShelfResponse;
import com.nexuswms.inventory.dto.response.ZoneResponse;
import com.nexuswms.inventory.entity.Aisle;
import com.nexuswms.inventory.entity.Shelf;
import com.nexuswms.inventory.entity.Zone;
import com.nexuswms.inventory.repository.AisleRepository;
import com.nexuswms.inventory.repository.ShelfRepository;
import com.nexuswms.inventory.repository.ZoneRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LocationService {

    private final ZoneRepository zoneRepository;
    private final AisleRepository aisleRepository;
    private final ShelfRepository shelfRepository;

    public LocationService(
            ZoneRepository zoneRepository,
            AisleRepository aisleRepository,
            ShelfRepository shelfRepository
    ) {
        this.zoneRepository = zoneRepository;
        this.aisleRepository = aisleRepository;
        this.shelfRepository = shelfRepository;
    }

    // --- Zones ---

    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        if (zoneRepository.existsByName(request.name())) {
            throw new ConflictException("Zone already exists: " + request.name());
        }
        Zone zone = Zone.builder()
                .name(request.name().toUpperCase())
                .type(request.type().toUpperCase())
                .capacity(request.capacity())
                .build();
        return ZoneResponse.from(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll()
                .stream()
                .map(ZoneResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ZoneResponse getZoneById(UUID id) {
        return zoneRepository.findById(id)
                .map(ZoneResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", id.toString()));
    }

    // --- Aisles ---

    @Transactional
    public AisleResponse createAisle(AisleRequest request) {
        if (aisleRepository.existsByCode(request.code())) {
            throw new ConflictException("Aisle code already exists: " + request.code());
        }
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone", request.zoneId().toString()));

        Aisle aisle = Aisle.builder()
                .zone(zone)
                .code(request.code().toUpperCase())
                .build();
        return AisleResponse.from(aisleRepository.save(aisle));
    }

    @Transactional(readOnly = true)
    public List<AisleResponse> getAislesByZone(UUID zoneId) {
        return aisleRepository.findByZoneId(zoneId)
                .stream()
                .map(AisleResponse::from)
                .toList();
    }

    // --- Shelves ---

    @Transactional
    public ShelfResponse createShelf(ShelfRequest request) {
        if (shelfRepository.existsByCode(request.code())) {
            throw new ConflictException("Shelf code already exists: " + request.code());
        }
        Aisle aisle = aisleRepository.findById(request.aisleId())
                .orElseThrow(() -> new ResourceNotFoundException("Aisle", request.aisleId().toString()));

        Shelf shelf = Shelf.builder()
                .aisle(aisle)
                .level(request.level().toUpperCase())
                .code(request.code().toUpperCase())
                .maxWeight(request.maxWeight())
                .build();
        return ShelfResponse.from(shelfRepository.save(shelf));
    }

    @Transactional(readOnly = true)
    public List<ShelfResponse> getShelvesByZone(UUID zoneId) {
        return shelfRepository.findByAisleZoneId(zoneId)
                .stream()
                .map(ShelfResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShelfResponse> getShelvesByAisle(UUID aisleId) {
        return shelfRepository.findByAisleId(aisleId)
                .stream()
                .map(ShelfResponse::from)
                .toList();
    }
}