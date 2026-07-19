/**
 * purchaseOrderService.ts
 * Thin HTTP layer over /purchase-orders. Mirrors PurchaseOrderController
 * (com.nexuswms.procurement.controller) — see phase3_progress.md.
 */
import http from '@/core/services/http';
import type { PurchaseOrder, CreatePurchaseOrderPayload } from '@/core/models/purchaseOrder';

export const purchaseOrderService = {
  list() {
    return http.get<PurchaseOrder[]>('/purchase-orders');
  },
  getById(id: string) {
    return http.get<PurchaseOrder>(`/purchase-orders/${id}`);
  },
  create(payload: CreatePurchaseOrderPayload) {
    return http.post<PurchaseOrder>('/purchase-orders', payload);
  },
  approve(id: string) {
    return http.post<PurchaseOrder>(`/purchase-orders/${id}/approve`);
  },
};