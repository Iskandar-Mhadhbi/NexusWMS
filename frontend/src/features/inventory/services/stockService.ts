/**
 * stockService — thin HTTP layer for the Stock tab (read-only pass).
 * Talks to StockController via the shared axios instance (`http`), which
 * already attaches the JWT bearer token.
 *
 * Deliberately excludes POST /stock/adjust and GET /stock/{skuId}/movements
 * this pass — adjustment needs a real shelf picker (blocked on Zones),
 * movement history wasn't in scope for the read-only pass.
 */
import http from '@/core/services/http';
import type { StockSummary, SkuLocation, ReorderAlert, AdjustStockPayload } from '@/core/models/stock';

export const stockService = {
  /** Fetch the aggregated stock summary across all SKUs. */
  async getSummary(): Promise<StockSummary[]> {
    const { data } = await http.get<StockSummary[]>('/stock');
    return data;
  },

  /** Fetch the per-shelf/batch location breakdown for one SKU. */
  async getLocations(skuId: string): Promise<SkuLocation[]> {
    const { data } = await http.get<SkuLocation[]>(`/stock/${skuId}/locations`);
    return data;
  },

  /** Fetch all currently open reorder alerts. */
  async getReorderAlerts(): Promise<ReorderAlert[]> {
    const { data } = await http.get<ReorderAlert[]>('/stock/alerts/reorder');
    return data;
  },

  async adjustStock(payload: AdjustStockPayload): Promise<SkuLocation> {
    const { data } = await http.post<SkuLocation>('/stock/adjust', payload);
    return data;
  },
};