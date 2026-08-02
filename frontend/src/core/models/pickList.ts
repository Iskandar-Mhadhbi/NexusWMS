/**
 * pickList.ts
 * Frontend mirror of com.nexuswms.fulfillment.dto.response.PickListResponse
 * / PickListItemResponse. Previously split into two near-duplicate type
 * pairs (PickList/PickListItem vs PickListResponse/PickListItemResponse)
 * with no actual reason for the split — no Zod schema ever consumed one
 * over the other. Merged into one canonical shape per entity, matching
 * the single-type convention used by Order/PurchaseOrder/Shipment.
 *
 * generatedBy/assignedTo are enriched UserSummary objects (actor
 * enrichment pass, see actor_field_enrichment_progress.md), not raw UUIDs.
 *
 * NOTE: skuCode/skuName on PickListItem remain unconfirmed against real
 * PickListItemResponse source — carried forward from the original file,
 * still genuinely unverified, not resolved by this merge.
 */
import type { UserSummary } from './user';

export type PickListStatus = 'GENERATED' | 'IN_PROGRESS' | 'COMPLETED';
export type PickListItemStatus = 'PENDING' | 'PICKED' | 'SKIPPED';

export interface PickListItem {
  id: string;
  orderLineId: string;
  skuId: string;
  skuCode?: string;
  skuName?: string;
  shelfId: string;
  shelfCode: string;
  quantityToPick: number;
  quantityPicked: number;
  batchId?: string | null;
  status: PickListItemStatus;
  pickedAt: string | null;
}

export interface PickList {
  id: string;
  fulfillmentRequestId: string;
  generatedBy: UserSummary | null;
  assignedTo: UserSummary;
  status: PickListStatus;
  generatedAt: string;
  completedAt: string | null;
  items: PickListItem[];
}