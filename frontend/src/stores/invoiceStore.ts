/**
 * Invoice state — Finance pillar.
 * Top-level store (not feature-scoped) in case Procurement ever wants to
 * surface invoice status alongside a PO, same reasoning as skuStore.
 */

import { defineStore } from 'pinia';
import { ref } from 'vue';
import { invoiceService } from '@/features/finance/services/invoiceService';
import type { InvoiceResponse, CreateInvoiceRequest } from '@/core/models/invoice';

export const useInvoiceStore = defineStore('invoice', () => {
  const invoices = ref<InvoiceResponse[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchAll() {
    loading.value = true;
    error.value = null;
    try {
      invoices.value = await invoiceService.getAll();
    } catch (e) {
      error.value = 'Failed to load invoices.';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function create(payload: CreateInvoiceRequest) {
    error.value = null;
    try {
      const created = await invoiceService.create(payload);
      invoices.value.push(created);
      return created;
    } catch (e) {
      error.value = 'Failed to create invoice.';
      throw e;
    }
  }

  /** Runs 3-way match and updates the invoice's status in place. */
  async function match(id: string) {
    error.value = null;
    try {
      const updated = await invoiceService.match(id);
      const idx = invoices.value.findIndex((inv) => inv.id === id);
      if (idx !== -1) invoices.value[idx] = updated;
      return updated;
    } catch (e) {
      error.value = 'Failed to match invoice.';
      throw e;
    }
  }

  return { invoices, loading, error, fetchAll, create, match };
});