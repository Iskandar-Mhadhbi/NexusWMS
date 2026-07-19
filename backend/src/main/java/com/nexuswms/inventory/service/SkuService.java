package com.nexuswms.inventory.service;

import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.inventory.dto.request.CategoryRequest;
import com.nexuswms.inventory.dto.request.SkuRequest;
import com.nexuswms.inventory.dto.response.CategoryResponse;
import com.nexuswms.inventory.dto.response.SkuResponse;
import com.nexuswms.inventory.entity.Category;
import com.nexuswms.inventory.entity.Sku;
import com.nexuswms.inventory.repository.CategoryRepository;
import com.nexuswms.inventory.repository.SkuRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set; 
import java.util.Objects;

@Service
public class SkuService {

    private final SkuRepository skuRepository;
    private final CategoryRepository categoryRepository;

    public SkuService(SkuRepository skuRepository, CategoryRepository categoryRepository) {
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
    }

    // --- Categories ---

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException("Category already exists: " + request.name());
        }
        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.parentId().toString()));
        }
        Category category = Category.builder()
            .name(request.name())
            .parent(parent)
            .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNull()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }

    // --- SKUs ---

    @Transactional
    public SkuResponse createSku(SkuRequest request) {
        if (skuRepository.existsBySkuCode(request.skuCode())) {
            throw new ConflictException("SKU code already exists: " + request.skuCode());
        }
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId().toString()));
        }
        Sku sku = Sku.builder()
                .skuCode(request.skuCode())
                .name(request.name())
                .category(category)
                .weightKg(request.weightKg())
                .dimensions(request.dimensions())
                .unit(request.unit())
                .reorderPoint(request.reorderPoint())
                .reorderQuantity(request.reorderQuantity())
                .build();
        return SkuResponse.from(skuRepository.save(sku));
    }

    @Transactional(readOnly = true)
    public SkuResponse getSkuByCode(String skuCode) {
        return skuRepository.findBySkuCode(skuCode)
                .map(SkuResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("SKU", skuCode));
    }

    @Transactional(readOnly = true)
    public SkuResponse getSkuById(UUID id) {
        return skuRepository.findById(id)
                .map(SkuResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("SKU", id.toString()));
    }

    @Transactional(readOnly = true)
    public List<SkuResponse> getAllSkus() {
        return skuRepository.findAll()
                .stream()
                .map(SkuResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkuResponse> searchSkus(String keyword) {
        return skuRepository.searchByKeyword(keyword)
                .stream()
                .map(SkuResponse::from)
                .toList();
    }
    @Transactional(readOnly = true)
    public Map<UUID, Sku> getSkuMapByIds(Set<UUID> ids) {
        return skuRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(
                    sku -> Objects.requireNonNull(sku.getId()),
                    sku -> sku
                ));
    }
    @Transactional(readOnly = true)
    public Map<UUID, SkuResponse> getSkuResponseMapByIds(Set<UUID> skuIds) {
        return skuRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(
                        sku -> Objects.requireNonNull(sku.getId()),
                        SkuResponse::from
                ));
    }
}