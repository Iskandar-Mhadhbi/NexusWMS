/**
 * skuService.ts
 * Thin HTTP layer over /skus and /skus/categories. Mirrors SkuController
 * (com.nexuswms.inventory.controller) — see progress.txt Phase 2.
 */
import http from '@/core/services/http';
import type { Sku, Category, CreateSkuPayload, CreateCategoryPayload } from '@/core/models/sku';

export const skuService = {
  listSkus() {
    return http.get<Sku[]>('/skus');
  },
  createSku(payload: CreateSkuPayload) {
    return http.post<Sku>('/skus', payload);
  },
  listCategories() {
    return http.get<Category[]>('/skus/categories');
  },
  createCategory(payload: CreateCategoryPayload) {
    return http.post<Category>('/skus/categories', payload);
  },
};