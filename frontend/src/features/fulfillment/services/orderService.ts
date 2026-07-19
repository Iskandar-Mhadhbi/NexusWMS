/**
 * orderService — thin HTTP layer for Orders (Fulfillment pillar).
 * Wraps OrderController.java's endpoints exactly — this file makes no
 * decisions about state or UI, it only shapes requests/responses.
 */
import  http  from '@/core/services/http';
import type { Order, CreateOrderPayload } from '@/core/models/order';

export const orderService = {
  /** GET /orders — list all orders, ADMIN/MANAGER only. Confirmed via OrderController.java. */
  async getAll(): Promise<Order[]> {
    const { data } = await http.get<Order[]>('/orders');
    return data;
  },

  /** GET /orders/{id} — single order with full line detail. */
  async getById(id: string): Promise<Order> {
    const { data } = await http.get<Order>(`/orders/${id}`);
    return data;
  },

  /** POST /orders — creates an order (status starts RECEIVED). Order number is server-generated. */
  async create(payload: CreateOrderPayload): Promise<Order> {
    const { data } = await http.post<Order>('/orders', payload);
    return data;
  },

  /**
   * POST /orders/{id}/cancel — only valid while status is RECEIVED or
   * VALIDATED; the backend rejects cancellation once PICKING or beyond.
   * Returns the updated Order with status CANCELLED.
   */
  async cancel(id: string): Promise<Order> {
    const { data } = await http.post<Order>(`/orders/${id}/cancel`);
    return data;
  },

  /**
   * POST /orders/{id}/fulfillment-request — transitions the order
   * RECEIVED -> VALIDATED and creates the FulfillmentRequest that
   * pick-list generation will later consume. Returns a
   * FulfillmentRequestResponse, NOT an OrderResponse — the order's own
   * updated status is not in this response body, so callers must refetch
   * the order separately to see the status change reflected.
   */
  async generateFulfillmentRequest(id: string): Promise<void> {
    await http.post(`/orders/${id}/fulfillment-request`);
  },
};