/**
 * Demand forecast API service — Forecast tab.
 * Wraps FastAPI GET /forecast/demand?days_ahead=N&sku_id=optional.
 */
import analyticsHttp from '@/core/services/analyticsHttp';
import type { DemandForecastResponse } from '@/core/models/analytics';

export const forecastService = {
  async getDemand(daysAhead: number, skuId?: string): Promise<DemandForecastResponse> {
    const params: Record<string, string | number> = { days_ahead: daysAhead };
    if (skuId) params.sku_id = skuId;
    const { data } = await analyticsHttp.get<DemandForecastResponse>('/forecast/demand', { params });
    return data;
  },
};