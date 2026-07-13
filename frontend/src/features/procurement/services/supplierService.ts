/**
 * supplierService.ts
 * Thin HTTP layer over /suppliers. Mirrors SupplierController
 * (com.nexuswms.procurement.controller) — see phase3_progress.md.
 * No business logic here — that lives in supplierStore.ts.
 */
import http from '@/core/services/http';
import type { Supplier, CreateSupplierPayload } from '@/core/models/supplier';

export const supplierService = {
  /** @returns all suppliers */
  list() {
    return http.get<Supplier[]>('/suppliers');
  },

  /** @returns a single supplier by UUID */
  getById(id: string) {
    return http.get<Supplier>(`/suppliers/${id}`);
  },

  /** Creates a supplier. Backend requires ADMIN or INVENTORY_CONTROLLER role. */
  create(payload: CreateSupplierPayload) {
    return http.post<Supplier>('/suppliers', payload);
  },
};