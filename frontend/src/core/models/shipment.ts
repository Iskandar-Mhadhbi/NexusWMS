/**
 * Shipment domain models — manager-facing dispatch oversight.
 * A Shipment is created once a Parcel is dispatched: carrier assigned,
 * carrier tracking number generated, dispatchedBy recorded from JWT principal.
 *
 * Confirmed against real Java source (ShipmentResponse.java) — parcelId
 * and carrierCode were missing here, now added. dispatchedBy is an
 * enriched UserSummary object (actor enrichment pass, see
 * actor_field_enrichment_progress.md), not a raw UUID.
 */
import type { UserSummary } from './user';

export type ShipmentStatus = 'PENDING' | 'DISPATCHED' | 'DELIVERED';

export interface ShipmentResponse {
  id: string;
  parcelId: string;
  parcelTrackingNumber: string;   // internal TRK-YYYYMMDD-XXXXX
  carrierId: string;
  carrierName: string;
  carrierCode: string;
  carrierTrackingNumber: string;  // {CARRIER_CODE}-XXXXXXXXXX
  dispatchedBy: UserSummary | null;
  dispatchedAt: string | null;
  estimatedDelivery: string | null;
  status: ShipmentStatus;
}