/**
 * purchaseOrderStore.ts
 * Same load-once-cache pattern as supplierStore.ts — see
 * phase7_frontend_design.md §7.
 */
import { defineStore } from 'pinia';
import { purchaseOrderService } from '@/features/procurement/services/purchaseOrderService';
import type { PurchaseOrder, CreatePurchaseOrderPayload } from '@/core/models/purchaseOrder';

export const usePurchaseOrderStore = defineStore('purchaseOrders', {
  state: () => ({
    purchaseOrders: [] as PurchaseOrder[],
    loaded: false,
    loading: false,
    error: null as string | null,
  }),

  actions: {
    async load(force = false) {
      if (this.loaded && !force) return;
      this.loading = true;
      this.error = null;
      try {
        const { data } = await purchaseOrderService.list();
        this.purchaseOrders = data;
        this.loaded = true;
      } catch (err) {
        this.error = 'Failed to load purchase orders';
        console.error('[purchaseOrderStore] load failed:', err);
      } finally {
        this.loading = false;
      }
    },

    async create(payload: CreatePurchaseOrderPayload) {
      const { data } = await purchaseOrderService.create(payload);
      this.purchaseOrders.push(data);
      return data;
    },

    /**
     * Approves a PO and replaces the local copy with the server's response
     * (status transitions server-side, e.g. DRAFT -> APPROVED).
     */
    async approve(id: string) {
      const { data } = await purchaseOrderService.approve(id);
      const index = this.purchaseOrders.findIndex((po) => po.id === id);
      if (index !== -1) {
        this.purchaseOrders[index] = data;
      }
      return data;
    },
  },
});