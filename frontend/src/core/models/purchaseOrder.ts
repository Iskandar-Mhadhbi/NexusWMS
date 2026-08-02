/**
 * purchaseOrder.ts
 * Frontend mirror of com.nexuswms.procurement.dto.response.PurchaseOrderResponse
 * / PurchaseOrderLineResponse and the matching Request DTOs.
 *
 * Confirmed against the real Java source (PurchaseOrderService.java,
 * PurchaseOrderResponse.java) — the earlier note about inferred field
 * names no longer applies.
 *
 * requestedBy/approvedBy are enriched UserSummary objects, not raw UUIDs
 * — part of the actor-enrichment pass (see actor_field_enrichment_progress.md).
 * approvedBy is null until the PO is actually approved.
 */
import type { UserSummary } from './user';

export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'PARTIALLY_RECEIVED'
  | 'FULLY_RECEIVED'
  | 'CANCELLED';

export type PurchaseOrderLineStatus = 'PENDING' | 'PARTIALLY_RECEIVED' | 'FULLY_RECEIVED';

export interface PurchaseOrderLine {
  id: string;
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
  supplierName: string;
  requestedBy: UserSummary;
  approvedBy: UserSummary | null;
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