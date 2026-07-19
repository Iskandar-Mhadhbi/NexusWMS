/**
 * Invoice domain models — Finance pillar.
 * Invoice is created against an approved PurchaseOrder, then matched
 * (3-way match: PO total vs. goods receipt vs. invoice amount) to confirm
 * payment eligibility.
 */

export type InvoiceStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type ThreeWayMatchStatus = 'PENDING' | 'MATCHED' | 'DISCREPANCY';

export interface InvoiceResponse {
  id: string;
  purchaseOrderId: string;
  poNumber: string;
  supplierId: string;
  supplierName: string;
  invoiceNumber: string;
  invoiceAmount: number;      // BigDecimal serializes as number over JSON
  status: InvoiceStatus;
  threeWayMatchStatus: ThreeWayMatchStatus;
  createdAt: string;          // ISO timestamp
}

import { z } from 'zod';

/**
 * Validation schema for creating an invoice.
 * Mirrors InvoiceRequest.java — purchaseOrderId, invoiceNumber, invoiceAmount.
 * TODO: confirm exact InvoiceRequest.java field names/constraints before
 * relying on this in production; built from the response DTO + controller
 * signature, not a confirmed request DTO paste.
 */
export const CreateInvoiceSchema = z.object({
  purchaseOrderId: z.uuid({ message: 'Select a purchase order' }),
  invoiceNumber: z.string().min(1, { error: 'Invoice number is required' }),
  invoiceAmount: z.number().positive({ error: 'Amount must be greater than 0' }),
});

export type CreateInvoiceRequest = z.infer<typeof CreateInvoiceSchema>;