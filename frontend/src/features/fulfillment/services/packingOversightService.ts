/**
 * Manager-facing packing task read service.
 * Distinct from features/packer/services/packingTaskService.ts, which
 * handles the worker's own start/complete action flow.
 *
 * TODO(backend): GET /packing-tasks (list-all) is UNCONFIRMED.
 * If it 404s, add a controller method mirroring PackingTaskRepository's
 * findByAssignedTo/findByStatus, @PreAuthorize("hasAnyRole('MANAGER','ADMIN')").
 */

import http  from '@/core/services/http';
import type { PackingTask, PackingTaskStatus } from '@/core/models/packingTask';

export const packingOversightService = {
  async getAll(status?: PackingTaskStatus): Promise<PackingTask[]> {
    const params = status ? { status } : undefined;
    const { data } = await http.get<PackingTask[]>('/packing-tasks', { params });
    return data;
  },

  async getById(id: string): Promise<PackingTask> {
    const { data } = await http.get<PackingTask>(`/packing-tasks/${id}`);
    return data;
  },
};