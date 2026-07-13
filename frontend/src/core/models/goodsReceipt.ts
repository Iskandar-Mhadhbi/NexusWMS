/**
 * goodsReceipt.ts
 * Frontend models for the receiving terminal — submitting a GoodsReceipt
 * against an APPROVED PurchaseOrder. Mirrors GoodsReceiptRequest /
 * GoodsReceiptLineRequest / GoodsReceiptResponse (see phase3_progress.md).
 *
 * shelfId is entered manually (same pattern as skuId in the Purchase
 * Orders form) since the Inventory pillar, which would resolve real shelf
 * codes, isn't built yet.
 */
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

export interface GoodsReceipt {
  id: string;
  grNumber: string;
  purchaseOrderId: string;
  receivedBy: string;
  receivedAt: string;
  notes: string;
}