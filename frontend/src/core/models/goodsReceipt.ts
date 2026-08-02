/**
 * goodsReceipt.ts
 * Frontend models for the receiving terminal — submitting a GoodsReceipt
 * against an APPROVED PurchaseOrder. Mirrors GoodsReceiptRequest /
 * GoodsReceiptLineRequest / GoodsReceiptResponse / GoodsReceiptLineResponse
 * (confirmed against real Java source, GoodsReceiptService.java /
 * GoodsReceiptResponse.java).
 *
 * shelfId is entered manually (same pattern as skuId in the Purchase
 * Orders form) since the Inventory pillar, which would resolve real shelf
 * codes, isn't built yet — skuCode/shelfCode on the response line are
 * null from the backend for the same reason, resolved by the frontend
 * via IDs elsewhere, not here.
 *
 * receivedBy is an enriched UserSummary object (actor enrichment pass,
 * see actor_field_enrichment_progress.md), not a raw UUID.
 */
import type { UserSummary } from './user';

export interface GoodsReceiptLinePayload {
  poLineId: string;
  skuId: string;
  quantityReceived: number;
  batchId: string;
  expiryDate: string | null;
  shelfId: string;
}

export interface CreateGoodsReceiptPayload {
  purchaseOrderId: string;
  notes: string;
  lines: GoodsReceiptLinePayload[];
}

export interface GoodsReceiptLine {
  id: string;
  poLineId: string;
  skuId: string;
  skuCode: string | null;
  quantityReceived: number;
  batchId: string;
  expiryDate: string | null;
  shelfId: string;
  shelfCode: string | null;
}

export interface GoodsReceipt {
  id: string;
  grNumber: string;
  purchaseOrderId: string;
  poNumber: string;
  receivedBy: UserSummary;
  receivedAt: string;
  notes: string;
  lines: GoodsReceiptLine[];
}