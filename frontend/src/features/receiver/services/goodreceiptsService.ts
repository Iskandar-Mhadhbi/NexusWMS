/**
 * goodsReceiptService.ts
 * Thin HTTP layer over /goods-receipts. Mirrors GoodsReceiptController
 * (com.nexuswms.procurement.controller) — see phase3_progress.md.
 */
import http from '@/core/services/http';
import type { GoodsReceipt, CreateGoodsReceiptPayload } from '@/core/models/goodsReceipt';

export const goodsReceiptService = {
  create(payload: CreateGoodsReceiptPayload) {
    return http.post<GoodsReceipt>('/goods-receipts', payload);
  },
};