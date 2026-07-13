/**
 * packingTaskService.ts
 * Thin HTTP layer over /packing-tasks, scoped to what the pack station
 * terminal needs.
 *
 * NOTE: getMyActiveTask() targets GET /packing-tasks/my, which is INFERRED
 * from the pick-lists/my naming convention — it is NOT confirmed as an
 * actual tested endpoint anywhere in the phase docs (only POST
 * /packing-tasks, .../start, .../complete were verified — see
 * phase5_progress.md). If this 404s, the real backend likely needs this
 * endpoint added, or the packer's task must be resolved a different way
 * (e.g. passed in from a manager-assignment notification instead of
 * self-fetched).
 */
import http from '@/core/services/http';
import type { PackingTask, ParcelSummary } from '@/core/models/packingTask';

export const packingTaskService = {
  getMyActiveTask() {
    return http.get<PackingTask>('/packing-tasks/my');
  },

  start(id: string) {
    return http.post<PackingTask>(`/packing-tasks/${id}/start`);
  },

  /**
   * Response shape here is genuinely uncertain — see file header. Typed
   * as an intersection so the store can read either a nested `parcel` or
   * fall back to just the task fields without a runtime error either way.
   */
  complete(id: string) {
    return http.post<PackingTask & { parcel?: ParcelSummary }>(`/packing-tasks/${id}/complete`);
  },
};