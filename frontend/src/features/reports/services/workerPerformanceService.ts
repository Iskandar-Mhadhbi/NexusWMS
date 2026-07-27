/**
 * Worker performance API service — Daily report tab.
 * Wraps FastAPI GET /worker/performance?target_date=YYYY-MM-DD.
 * target_date is a real, working single-day param — not a range.
 * Defaults to today server-side when omitted.
 */
import analyticsHttp from '@/core/services/analyticsHttp';
import type { WorkerPerformanceResponse } from '@/core/models/analytics';

export const workerPerformanceService = {
  async getByDate(targetDate?: string): Promise<WorkerPerformanceResponse> {
    const params = targetDate ? { target_date: targetDate } : undefined;
    const { data } = await analyticsHttp.get<WorkerPerformanceResponse>('/worker/performance', { params });
    return data;
  },
};