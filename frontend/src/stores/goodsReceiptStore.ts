/**
 * goodsReceiptStore.ts
 * Owns the receiving terminal's submission state only. The purchase-order
 * list itself is read from purchaseOrderStore (already built for
 * Procurement) rather than duplicated here — same PO data, a second
 * consumer.
 */
import { defineStore } from 'pinia';
import { goodsReceiptService } from '@/features/receiver/services/goodreceiptsService';
import type { CreateGoodsReceiptPayload, GoodsReceipt } from '@/core/models/goodsReceipt';

export const useGoodsReceiptStore = defineStore('goodsReceipt', {
  state: () => ({
    submitting: false,
    error: null as string | null,
    lastReceipt: null as GoodsReceipt | null,
  }),

  actions: {
    /** Submits a full goods receipt (header + all lines) in one call. */
    async submit(payload: CreateGoodsReceiptPayload) {
      this.submitting = true;
      this.error = null;
      try {
        const { data } = await goodsReceiptService.create(payload);
        this.lastReceipt = data;
        return data;
      } catch (err) {
        this.error = 'Could not submit goods receipt — check the fields and try again.';
        console.error('[goodsReceiptStore] submit failed:', err);
        throw err;
      } finally {
        this.submitting = false;
      }
    },

    /** Clears the last-submitted receipt so the terminal can start a new one. */
    reset() {
      this.lastReceipt = null;
      this.error = null;
    },
  },
});