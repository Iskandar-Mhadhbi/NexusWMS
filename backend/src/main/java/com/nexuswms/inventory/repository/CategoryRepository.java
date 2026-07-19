package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    List<Category> findByParentIsNull();
    List<Category> findByParentId(UUID parentId);
}