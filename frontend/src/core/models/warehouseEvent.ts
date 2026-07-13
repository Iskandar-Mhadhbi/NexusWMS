/**
 * warehouseEvent.ts
 *
 * Shape of events broadcast over /topic/dashboard and /topic/alerts.
 * Mirrors the backend's WarehouseEvent record (com.nexuswms.dashboard),
 * which intentionally uses only String fields for every property — anything
 * crossing a process boundary (Redis pub/sub, WebSocket) should be a
 * primitive, not a domain type (see phase6_progress.md).
 *
 * NOTE: inferred from tested payloads in phase5/phase6_progress.md, not
 * from the actual WarehouseEvent.java source — confirm field names against
 * the real backend record if this ever needs to be authoritative.
 */
export type WarehouseEventType =
  | 'ORDER_CREATED'
  | 'ORDER_VALIDATED'
  | 'ORDER_CANCELLED'
  | 'ITEM_PICKED'
  | 'PARCEL_PACKED'
  | 'ORDER_DISPATCHED'
  | 'REORDER_ALERT';

export interface WarehouseEvent {
  type: WarehouseEventType;
  referenceId: string;
  status: string;
  workerId: string;
  zone: string;
  timestamp: string;
}