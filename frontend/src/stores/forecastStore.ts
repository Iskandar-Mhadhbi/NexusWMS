/**
 * Demand forecast state — Reports pillar, Forecast tab.
 */
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { forecastService } from '@/features/reports/services/forecastService';
import type { DemandForecastResponse } from '@/core/models/analytics';

export const useForecastStore = defineStore('forecast', () => {
  const forecast = ref<DemandForecastResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetch(daysAhead: number, skuId?: string) {
    loading.value = true;
    error.value = null;
    try {
      forecast.value = await forecastService.getDemand(daysAhead, skuId);
    } catch (e) {
      error.value = 'Failed to load demand forecast.';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  return { forecast, loading, error, fetch };
});