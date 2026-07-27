/**
 * Stock valuation API service — Daily report tab.
 * Wraps FastAPI GET /stock/valuation. No date param — always a
 * current-moment snapshot, unlike worker/performance.
 */
import analyticsHttp from '@/core/services/analyticsHttp';
import type { StockValuationResponse } from '@/core/models/analytics';

export const stockValuationService = {
  async get(): Promise<StockValuationResponse> {
    const { data } = await analyticsHttp.get<StockValuationResponse>('/stock/valuation');
    return data;
  },
};