/**
 * dispatch.ts
 * Frontend models for the dispatch desk — parcels awaiting dispatch,
 * active carriers, and the resulting shipment. Matches the confirmed
 * dispatch flow: parcel PACKED -> DISPATCHED, dispatchedBy from JWT
 * (see phase5_progress.md).
 */
export type ParcelStatus = 'PACKED' | 'DISPATCHED';

export interface Parcel {
  id: string;
  orderId: string;
  trackingNumber: string;
  barcode: string;
  status: ParcelStatus;
  weightKg: number | null;
}

export interface Carrier {
  id: string;
  name: string;
  code: string;
  isActive: boolean;
}

export interface Shipment {
  id: string;
  parcelId: string;
  carrierId: string;
  carrierTrackingNumber: string;
  dispatchedBy: string;
  dispatchedAt: string;
  status: string;
}