/**
 * Shipment domain models — manager-facing dispatch oversight.
 * A Shipment is created once a Parcel is dispatched: carrier assigned,
 * carrier tracking number generated, dispatchedBy recorded from JWT principal.
 */

export type ShipmentStatus = 'PENDING' | 'DISPATCHED' | 'DELIVERED';

export interface ShipmentResponse {
  id: string;
  parcelId: string;
  parcelTrackingNumber: string;   // internal TRK-YYYYMMDD-XXXXX
  carrierId: string;
  carrierName: string;
  carrierTrackingNumber: string;  // {CARRIER_CODE}-XXXXXXXXXX
  dispatchedBy: string | null;
  dispatchedAt: string | null;
  estimatedDelivery: string | null;
  status: ShipmentStatus;
}