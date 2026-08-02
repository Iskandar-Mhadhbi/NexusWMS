/**
 * packingTaskService.ts
 * Thin HTTP layer over /packing-tasks, scoped to what the pack station
 * terminal needs.
 *
 * NOTE: getMyActiveTask() targets GET /packing-tasks/my, which is INFERRED
 * from the pick-lists/my naming convention — it is NOT confirmed as an
 * actual tested endpoint (only POST /packing-tasks, .../start,
 * .../complete were verified — see phase5_progress.md). If this 404s,
 * the real backend likely needs this endpoint added, or the packer's
 * task must be resolved a different way (e.g. passed in from a manager-
 * assignment notification instead of self-fetched).
 *
 * complete() response shape CONFIRMED against real backend source
 * (PackingService.completeTask() returns ParcelResponse directly — no
 * nested task, no intersection type needed; that was a defensive hedge
 * against uncertainty that no longer applies). complete() now also sends
 * the required PackingCompleteRequest body (weightKg, dimensions) — the
 * original call sent no body at all, which the real backend requires.
 */
import http from '@/core/services/http';
import type { PackingTask, ParcelSummary } from '@/core/models/packingTask';

export interface CompletePackingTaskPayload {
  weightKg: number;
  dimensions: Record<string, unknown>;
}

export const packingTaskService = {
  getMyActiveTask() {
    return http.get<PackingTask>('/packing-tasks/my');
  },

  start(id: string) {
    return http.post<PackingTask>(`/packing-tasks/${id}/start`);
  },

  complete(id: string, payload: CompletePackingTaskPayload) {
    return http.post<ParcelSummary>(`/packing-tasks/${id}/complete`, payload);
  },
};