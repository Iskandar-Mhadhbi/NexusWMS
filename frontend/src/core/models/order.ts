/**
 * Order domain models. Fields confirmed against real OrderResponse.java
 * (id, orderNumber, customerName, customerAddress, status, priority, notes,
 * lines, totalAmount, createdAt, updatedAt). OrderLine.lineTotal matches
 * the same computed-field convention already used by PurchaseOrderLineResponse.
 */
import { z } from 'zod';

export type OrderStatus = 'RECEIVED' | 'VALIDATED' | 'PICKING' | 'PACKING' | 'DISPATCHED' | 'CANCELLED';
export type OrderPriority = 'STANDARD' | 'EXPRESS' | 'URGENT';

export interface OrderLine {
  id: string;
  skuId: string;
  skuCode: string | null; // TODO: unconfirmed whether OrderLineResponse enriches this
  skuName: string | null;
  quantityOrdered: number;
  quantityPicked: number;
  quantityPacked: number;
  unitPrice: number;
  lineTotal: number | null;
  status: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  customerName: string;
  customerAddress: Record<string, unknown> | null;
  status: OrderStatus;
  priority: OrderPriority;
  notes: string | null;
  lines: OrderLine[];
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
}

export const CreateOrderLineSchema = z.object({
  skuId: z.string().uuid(),
  quantityOrdered: z.number().int().positive('Quantity must be at least 1'),
  unitPrice: z.number().min(0, 'Unit price cannot be negative'),
});

/**
 * customerAddress defaults to {} — same deliberate choice SkusView.vue
 * made for Sku.dimensions: a flexible JSONB field with no confirmed
 * sub-shape, deferred rather than guessed at.
 */
export const CreateOrderSchema = z.object({
  customerName: z.string().trim().min(1, 'Customer name is required'),
  customerAddress: z.record(z.string(), z.unknown()).default({}),
  priority: z.enum(['STANDARD', 'EXPRESS', 'URGENT']),
  notes: z.string().trim().nullable(),
  lines: z.array(CreateOrderLineSchema).min(1, 'At least one order line is required'),
});
export type CreateOrderPayload = z.infer<typeof CreateOrderSchema>;