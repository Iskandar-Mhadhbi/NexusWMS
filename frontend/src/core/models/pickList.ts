/**
 * pickList.ts
 * Frontend mirror of com.nexuswms.fulfillment.dto.response.PickListResponse
 * / PickListItemResponse (see phase4_progress.md).
 *
 * NOTE: skuCode on PickListItem is inferred, not confirmed against real
 * source — phase4_progress.md says the response "includes shelf location
 * info" but doesn't give an exact field list. If the terminal shows a raw
 * UUID instead of a SKU code, this is the field to check first.
 */
export type PickListStatus = 'GENERATED' | 'IN_PROGRESS' | 'COMPLETED';
export type PickListItemStatus = 'PENDING' | 'PICKED' | 'SKIPPED';

export interface PickListItem {
  id: string;
  orderLineId: string;
  skuId: string;
  skuCode?: string;
  shelfId: string;
  shelfCode: string;
  quantityToPick: number;
  quantityPicked: number;
  batchId?: string;
  status: PickListItemStatus;
  pickedAt: string | null;
}

export interface PickList {
  id: string;
  fulfillmentRequestId: string;
  assignedTo: string;
  status: PickListStatus;
  generatedAt: string;
  completedAt: string | null;
  items: PickListItem[];
}