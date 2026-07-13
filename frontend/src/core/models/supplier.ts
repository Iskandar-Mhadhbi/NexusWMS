/**
 * supplier.ts
 * Frontend mirror of com.nexuswms.procurement.dto.response.SupplierResponse
 * and SupplierRequest (see phase3_progress.md). contactInfo is a loosely
 * typed passthrough of the backend's JSONB column — shape isn't strictly
 * enforced on the frontend since the backend doesn't constrain it either.
 *
 * NOTE: field names are taken from phase3_progress.md's DTO summary, not
 * the actual Java source — if create() fails validation on first real
 * submit, check this file against the real SupplierRequest first.
 */
export type SupplierStatus = 'ACTIVE' | 'INACTIVE' | 'BLACKLISTED';

export interface ContactInfo {
  phone?: string;
  address?: string;
  contactName?: string;
  [key: string]: unknown;
}

export interface Supplier {
  id: string;
  name: string;
  code: string;
  contactInfo: ContactInfo;
  paymentTerms: string;
  rating: number;
  status: SupplierStatus;
}

export interface CreateSupplierPayload {
  name: string;
  code: string;
  contactInfo: ContactInfo;
  paymentTerms: string;
  rating: number;
}