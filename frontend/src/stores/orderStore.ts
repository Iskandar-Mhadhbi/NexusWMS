/**
 * orderStore — top-level Pinia store for Orders (Fulfillment pillar).
 * Same shape as supplierStore/skuStore: flat list + loading/creating
 * flags + error string, read by OrdersView.vue.
 *
 * SYNC PATTERN: both cancel() and generateFulfillmentRequest() refetch
 * the whole list after their action, rather than patching a single row
 * from the response body. This is deliberate, not just cautious —
 * generateFulfillmentRequest's endpoint returns a FulfillmentRequestResponse,
 * not an OrderResponse, so the order's own updated status literally isn't
 * in that response at all; refetch is the only way to see it. cancel()
 * follows the same pattern for consistency, even though its own response
 * does carry a full Order — one sync rule for every action beats two
 * different rules depending on which endpoint happens to return what.
 */
import { defineStore } from 'pinia';
import { orderService } from '@/features/fulfillment/services/orderService';
import type { Order, CreateOrderPayload } from '@/core/models/order';

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: [] as Order[],
    loading: false,
    creating: false,
    error: null as string | null,
  }),

  actions: {
    /** Fetch all orders. Call on OrdersView mount. */
    async fetchAll() {
      this.loading = true;
      this.error = null;
      try {
        this.orders = await orderService.getAll();
      } catch (err) {
        this.error = 'Failed to load orders.';
        console.error(err);
      } finally {
        this.loading = false;
      }
    },

    /** Create a new order, then refresh the list. */
    async create(payload: CreateOrderPayload) {
      this.creating = true;
      this.error = null;
      try {
        await orderService.create(payload);
        await this.fetchAll();
      } catch (err) {
        this.error = 'Failed to create order.';
        console.error(err);
        throw err; // re-throw so the form can show its own message too
      } finally {
        this.creating = false;
      }
    },

    /**
     * Cancel an order, then refetch. Backend only allows this while status
     * is RECEIVED or VALIDATED — a rejection here (already PICKING+) is a
     * real, expected error, not a bug.
     */
    async cancel(id: string) {
      this.error = null;
      try {
        await orderService.cancel(id);
        await this.fetchAll();
      } catch (err) {
        this.error = 'Failed to cancel order.';
        console.error(err);
        throw err;
      }
    },

    /**
     * Generate a fulfillment request for an order (RECEIVED -> VALIDATED).
     * Must refetch — see file header comment for why.
     */
    async generateFulfillmentRequest(id: string) {
      this.error = null;
      try {
        await orderService.generateFulfillmentRequest(id);
        await this.fetchAll();
      } catch (err) {
        this.error = 'Failed to generate fulfillment request.';
        console.error(err);
        throw err;
      }
    },
  },
});