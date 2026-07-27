/**
 * Daily report state — Reports pillar, Daily tab.
 * Combines worker performance (single-day lookback, real param) and
 * stock valuation (always current, no historical query available).
 * Deliberately excludes throughput/orders-today/active-workers — those
 * are Live Ops's territory (WebSocket-live), not a query-driven report.
 */
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { workerPerformanceService } from '@/features/reports/services/workerPerformanceService';
import { stockValuationService } from '@/features/reports/services/stockValuationService';
import type { WorkerPerformanceResponse, StockValuationResponse } from '@/core/models/analytics';

export const useDailyReportStore = defineStore('dailyReport', () => {
  const workerPerformance = ref<WorkerPerformanceResponse | null>(null);
  const stockValuation = ref<StockValuationResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  /**
   * Loads both panels together. targetDate scopes worker performance only
   * (YYYY-MM-DD); stock valuation always reflects the current moment
   * regardless of targetDate.
   */
  async function fetchAll(targetDate?: string) {
    loading.value = true;
    error.value = null;
    try {
      const [workers, stock] = await Promise.all([
        workerPerformanceService.getByDate(targetDate),
        stockValuationService.get(),
      ]);
      workerPerformance.value = workers;
      stockValuation.value = stock;
    } catch (e) {
      error.value = 'Failed to load daily report.';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  return { workerPerformance, stockValuation, loading, error, fetchAll };
});