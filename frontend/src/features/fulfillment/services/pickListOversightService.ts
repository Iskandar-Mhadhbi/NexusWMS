/**
 * Manager-facing pick list read service.
 * Distinct from features/picker/services/pickListService.ts, which handles
 * the worker's own "/my" scoped fetch-and-pick flow.
 *
 * TODO(backend): GET /pick-lists is UNCONFIRMED — only /my and /{id} were
 * verified working in Phase 5 testing. If this 404s, add the endpoint:
 * mirror PickListRepository.findByStatus / findByAssignedTo into a new
 * PickListController method, @PreAuthorize("hasAnyRole('MANAGER','ADMIN')").
 */

import  http  from '@/core/services/http';
import type { PickListResponse, PickListStatus } from '@/core/models/pickList';

export const pickListOversightService = {
  /** Fetch all pick lists, optionally filtered by status. */
  async getAll(status?: PickListStatus): Promise<PickListResponse[]> {
    const params = status ? { status } : undefined;
    const { data } = await http.get<PickListResponse[]>('/pick-lists', { params });
    return data;
  },

  /** Fetch a single pick list with its items. */
  async getById(id: string): Promise<PickListResponse> {
    const { data } = await http.get<PickListResponse>(`/pick-lists/${id}`);
    return data;
  },
};