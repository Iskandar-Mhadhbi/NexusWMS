/**
 * skuStore.ts
 * Owns the SKU catalog + category list. Same load-once-cache pattern as
 * supplierStore.ts. Kept top-level (not feature-scoped) so Purchase Orders
 * and Goods Receipts can consume it once retrofitted with a real SKU
 * picker — see phase7_frontend_design.md open items.
 */
import { defineStore } from 'pinia';
import { skuService } from '@/features/inventory/services/skuService';
import type { Sku, Category, CreateSkuPayload, CreateCategoryPayload } from '@/core/models/sku';

export const useSkuStore = defineStore('skus', {
  state: () => ({
    skus: [] as Sku[],
    categories: [] as Category[],
    loaded: false,
    loading: false,
    error: null as string | null,
  }),

  getters: {
    /** @returns a category's display name for a given id, or the id itself if not found */
    categoryName: (state) => (categoryId: string | null): string => {
      if (!categoryId) return '—';
      return state.categories.find((c) => c.id === categoryId)?.name ?? categoryId;
    },
  },

  actions: {
    async load(force = false) {
      if (this.loaded && !force) return;
      this.loading = true;
      this.error = null;
      try {
        const [skusRes, categoriesRes] = await Promise.all([
          skuService.listSkus(),
          skuService.listCategories(),
        ]);
        this.skus = skusRes.data;
        this.categories = categoriesRes.data;
        this.loaded = true;
      } catch (err) {
        this.error = 'Failed to load SKU catalog';
        console.error('[skuStore] load failed:', err);
      } finally {
        this.loading = false;
      }
    },

    async createSku(payload: CreateSkuPayload) {
      const { data } = await skuService.createSku(payload);
      this.skus.push(data);
      return data;
    },

    async createCategory(payload: CreateCategoryPayload) {
      const { data } = await skuService.createCategory(payload);
      this.categories.push(data);
      return data;
    },
  },
});