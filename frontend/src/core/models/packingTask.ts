/**
 * packingTask.ts
 * Frontend mirror of com.nexuswms.fulfillment.dto.response.PackingTaskResponse.
 * Previously split into PackingTask/PackingTaskResponse with drifting
 * field sets — merged into one canonical shape, matching the single-type
 * convention used elsewhere.
 *
 * CORRECTION: parcelId/trackingNumber were previously (incorrectly)
 * speculated as fields on this response. Confirmed against real backend
 * source: POST /packing-tasks/{id}/complete returns a separate
 * ParcelResponse object entirely — PackingTaskResponse never carries
 * parcel data, nested or flat. Removed. Use ParcelSummary (below) as its
 * own response type instead.
 *
 * assignedTo/startedBy are enriched UserSummary objects (actor enrichment
 * pass, see actor_field_enrichment_progress.md), not raw UUIDs.
 */
import type { UserSummary } from './user';

export type PackingTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export interface PackingTask {
  id: string;
  pickListId: string;
  taskNumber: string;
  assignedTo: UserSummary;
  startedBy: UserSummary | null;
  stationId: string | null;
  stationCode: string | null;
  status: PackingTaskStatus;
  startedAt: string | null;
  completedAt: string | null;
}

/**
 * Shape returned by POST /packing-tasks/{id}/complete — a Parcel, not a
 * PackingTask. Confirmed against real ParcelResponse.from() usage in
 * PackingService.completeTask().
 */
export interface ParcelSummary {
  id: string;
  trackingNumber: string;
  barcode: string;
}