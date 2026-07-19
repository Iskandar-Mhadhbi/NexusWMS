/**
 * purchaseOrder.ts
 * Frontend mirror of com.nexuswms.procurement.dto.response.PurchaseOrderResponse
 * / PurchaseOrderLineResponse and the matching Request DTOs (see
 * phase3_progress.md).
 *
 * NOTE: field names are taken from phase3_progress.md's DTO summary, not
 * the actual Java source. In particular, whether PurchaseOrderLineResponse
 * includes an enriched skuCode (via PurchaseOrderService's SkuService
 * lookup) is inferred, not confirmed — treated as optional here; the view
 * falls back to the raw skuId if it's absent.
 */
export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'PARTIALLY_RECEIVED'
  | 'FULLY_RECEIVED'
  | 'CANCELLED';

export type PurchaseOrderLineStatus = 'PENDING' | 'PARTIALLY_RECEIVED' | 'FULLY_RECEIVED';

export interface PurchaseOrderLine {
  id: string; // ADDED — needed as goods-receipt's poLineId reference.
              // Very likely correct (every persisted entity in this system
              // exposes its id in the response DTO), but not confirmed
              // against the real PurchaseOrderLineResponse source.
  skuId: string;
  skuCode?: string;
  quantityOrdered: number;
  quantityReceived: number;
  unitPrice: number;
  status: PurchaseOrderLineStatus;
  lineTotal: number;
}

export interface PurchaseOrder {
  id: string;
  poNumber: string;
  supplierId: string;
  status: PurchaseOrderStatus;
  expectedDelivery: string | null;
  totalAmount: number;
  createdAt: string;
  lines: PurchaseOrderLine[];
}

export interface CreatePurchaseOrderLinePayload {
  skuId: string;
  quantityOrdered: number;
  unitPrice: number;
}

export interface CreatePurchaseOrderPayload {
  supplierId: string;
  expectedDelivery: string | null;
  lines: CreatePurchaseOrderLinePayload[];
}