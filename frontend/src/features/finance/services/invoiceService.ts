/**
 * Invoice API service — Finance pillar.
 * Wraps InvoiceController: create, match (3-way), list, get by id.
 */

import   http   from '@/core/services/http';
import type { InvoiceResponse, CreateInvoiceRequest } from '@/core/models/invoice';

export const invoiceService = {
  async create(payload: CreateInvoiceRequest): Promise<InvoiceResponse> {
    const { data } = await http.post<InvoiceResponse>('/invoices', payload);
    return data;
  },

  /** Triggers 3-way match: PO total vs. goods receipt vs. invoice amount. */
  async match(id: string): Promise<InvoiceResponse> {
    const { data } = await http.post<InvoiceResponse>(`/invoices/${id}/match`);
    return data;
  },

  async getAll(): Promise<InvoiceResponse[]> {
    const { data } = await http.get<InvoiceResponse[]>('/invoices');
    return data;
  },

  async getById(id: string): Promise<InvoiceResponse> {
    const { data } = await http.get<InvoiceResponse>(`/invoices/${id}`);
    return data;
  },
};