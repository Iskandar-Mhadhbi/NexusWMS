package com.nexuswms.inventory.dto.response;

import com.nexuswms.inventory.entity.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        UUID parentId,
        String parentName
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getParent() != null ? category.getParent().getName() : null
        );
    }
}