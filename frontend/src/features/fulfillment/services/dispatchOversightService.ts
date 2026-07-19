/**
 * Manager-facing shipment read service.
 * Distinct from features/dispatcher/services/dispatchService.ts, which
 * handles the worker's own dispatch-queue action flow (carrier select +
 * confirm dispatch against GET /parcels?status=PACKED).
 *
 * TODO(backend): GET /shipments (list-all) is UNCONFIRMED — only
 * GET /parcels/{id}/shipment (single, by parcel) was verified in Phase 5.
 * If this 404s, add a controller method generalizing
 * ShipmentRepository.findByPackage_Id into a findAll/findByStatus,
 * @PreAuthorize("hasAnyRole('MANAGER','ADMIN')").
 */

import  http  from '@/core/services/http';
import type { ShipmentResponse, ShipmentStatus } from '@/core/models/shipment';

export const dispatchOversightService = {
  async getAll(status?: ShipmentStatus): Promise<ShipmentResponse[]> {
    const params = status ? { status } : undefined;
    const { data } = await http.get<ShipmentResponse[]>('/shipments', { params });
    return data;
  },

  async getById(id: string): Promise<ShipmentResponse> {
    const { data } = await http.get<ShipmentResponse>(`/shipments/${id}`);
    return data;
  },
};