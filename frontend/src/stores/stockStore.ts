/**
 * stockStore — top-level Pinia store for the Stock tab.
 * Holds the stock summary list, open reorder alerts (enriched with
 * skuCode/skuName client-side — see rationale in core/models/stock.ts),
 * and a per-SKU location cache so re-expanding an already-viewed row
 * doesn't re-fetch.
 */
import { defineStore } from 'pinia';
import { stockService } from '@/features/inventory/services/stockService';
import type { StockSummary, SkuLocation, ReorderAlert, AdjustStockPayload } from '@/core/models/stock';

interface ReorderAlertEnriched extends ReorderAlert {
  skuCode: string | null;
  skuName: string | null;
}

export const useStockStore = defineStore('stock', {
  state: () => ({
    levels: [] as StockSummary[],
    reorderAlerts: [] as ReorderAlert[],
    /** Per-SKU location breakdown cache, keyed by skuId. */
    locationsBySku: {} as Record<string, SkuLocation[]>,
    loadingLevels: false,
    loadingAlerts: false,
    loadingLocations: false,
    error: null as string | null,
  }),

  getters: {
    /**
     * Reorder alerts enriched with skuCode/skuName by cross-referencing
     * the already-loaded stock summary, since the alert response itself
     * isn't confirmed to carry SKU display fields (see model TODO).
     */
    reorderAlertsEnriched(state): ReorderAlertEnriched[] {
      return state.reorderAlerts.map((alert) => {
        const sku = state.levels.find((s) => s.skuId === alert.skuId);
        return {
          ...alert,
          skuCode: sku?.skuCode ?? null,
          skuName: sku?.skuName ?? null,
        };
      });
    },

    needsReorderCount(state): number {
      return state.levels.filter((s) => s.needsReorder).length;
    },
  },

  actions: {
    /** Fetch the stock summary table. Call on view mount. */
    async fetchLevels() {
      this.loadingLevels = true;
      this.error = null;
      try {
        this.levels = await stockService.getSummary();
      } catch (err) {
        this.error = 'Failed to load stock levels.';
        console.error(err);
      } finally {
        this.loadingLevels = false;
      }
    },

    /** Fetch open reorder alerts. Call on view mount, alongside fetchLevels. */
    async fetchReorderAlerts() {
      this.loadingAlerts = true;
      try {
        this.reorderAlerts = await stockService.getReorderAlerts();
      } catch (err) {
        this.error = 'Failed to load reorder alerts.';
        console.error(err);
      } finally {
        this.loadingAlerts = false;
      }
    },

    /**
     * Fetch (and cache) the location breakdown for one SKU. Used when a
     * stock table row is expanded. Skips the network call if already cached.
     */
    async fetchLocations(skuId: string) {
      if (this.locationsBySku[skuId]) return;
      this.loadingLocations = true;
      try {
        this.locationsBySku[skuId] = await stockService.getLocations(skuId);
      } catch (err) {
        this.error = 'Failed to load SKU locations.';
        console.error(err);
      } finally {
        this.loadingLocations = false;
      }
    },

    /**
     * Adjusts stock for a SKU at a shelf, then refreshes both the summary
     * table and that SKU's cached location breakdown so the UI reflects the
     * new quantity immediately — no full page reload needed.
     */
    async adjustStock(skuId: string, payload: AdjustStockPayload) {
      this.error = null;
      try {
        await stockService.adjustStock(payload);
        await this.fetchLevels();
        delete this.locationsBySku[skuId]; // force a fresh fetch, drop the stale cache
        await this.fetchLocations(skuId);
        await this.fetchReorderAlerts(); // an adjustment can open or resolve an alert
      } catch (err) {
        this.error = 'Failed to adjust stock.';
        console.error(err);
        throw err; // re-throw so the calling form can show its own message too
      }
    },
  },
});