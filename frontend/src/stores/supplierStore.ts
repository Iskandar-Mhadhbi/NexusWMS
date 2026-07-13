/**
 * supplierStore.ts
 * Caches the supplier list per phase7_frontend_design.md §7 — pillar data
 * loads once and is reused across navigation, not re-fetched every visit
 * to /procurement.
 */
import { defineStore } from 'pinia';
import { supplierService } from '@/features/procurement/services/supplierService';
import type { Supplier, CreateSupplierPayload } from '@/core/models/supplier';

export const useSupplierStore = defineStore('suppliers', {
  state: () => ({
    suppliers: [] as Supplier[],
    loaded: false,
    loading: false,
    error: null as string | null,
  }),

  actions: {
    /**
     * Fetches the supplier list once; subsequent calls are a no-op unless
     * `force` is passed — e.g. after navigating away and back deliberately
     * wanting fresh data.
     */
    async load(force = false) {
      if (this.loaded && !force) return;
      this.loading = true;
      this.error = null;
      try {
        const { data } = await supplierService.list();
        this.suppliers = data;
        this.loaded = true;
      } catch (err) {
        this.error = 'Failed to load suppliers';
        console.error('[supplierStore] load failed:', err);
      } finally {
        this.loading = false;
      }
    },

    /** Creates a supplier and appends it to local state on success. */
    async create(payload: CreateSupplierPayload) {
      const { data } = await supplierService.create(payload);
      this.suppliers.push(data);
      return data;
    },
  },
});