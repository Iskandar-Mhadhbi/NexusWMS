/**
 * dispatchService.ts
 * HTTP layer for the dispatch desk. Carrier endpoints are confirmed
 * tested (phase5_progress.md). Pending-parcels queue is the frontend's
 * stated need — backend TODO: GET /parcels?status=PACKED or equivalent.
 */
import http from '@/core/services/http';
import type { Parcel, Carrier, Shipment } from '@/core/models/dispatch';

export const dispatchService = {
  listPendingParcels() {
    return http.get<Parcel[]>('/parcels', { params: { status: 'PACKED' } });
  },

  listActiveCarriers() {
    return http.get<Carrier[]>('/carriers/active');
  },

  dispatch(parcelId: string, carrierId: string) {
    return http.post<Shipment>(`/parcels/${parcelId}/dispatch`, { carrierId });
  },
};