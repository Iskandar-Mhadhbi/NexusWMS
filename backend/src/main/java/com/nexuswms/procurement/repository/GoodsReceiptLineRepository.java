package com.nexuswms.procurement.repository;

import com.nexuswms.procurement.entity.GoodsReceiptLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, UUID> {

    List<GoodsReceiptLine> findByGoodsReceipt_Id(UUID goodsReceiptId);
}