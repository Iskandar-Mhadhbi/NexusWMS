package com.nexuswms.inventory.repository;

import com.nexuswms.inventory.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuRepository extends JpaRepository<Sku, UUID> {
    Optional<Sku> findBySkuCode(String skuCode);
    boolean existsBySkuCode(String skuCode);
    List<Sku> findByCategoryId(UUID categoryId);

    @Query("""
        SELECT s FROM Sku s
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(s.skuCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Sku> searchByKeyword(String keyword);
}