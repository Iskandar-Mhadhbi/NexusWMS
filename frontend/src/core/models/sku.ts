/**
 * sku.ts
 * Frontend mirror of the SKU catalog DTOs — CategoryRequest/Response,
 * SkuRequest/Response (com.nexuswms.inventory) — see progress.txt Phase 2.
 *
 * NOTE: field names taken from progress.txt's schema/DTO summary, not the
 * actual Java source. dimensions is a loose passthrough of the backend's
 * JSONB column, same pattern as Supplier.contactInfo.
 */
export interface Category {
  id: string;
  name: string;
  parentId: string | null;
}

export interface SkuDimensions {
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  [key: string]: unknown;
}

export interface Sku {
  id: string;
  skuCode: string;
  name: string;
  categoryId: string | null;
  weightKg: number;
  dimensions: SkuDimensions;
  unit: string;
  reorderPoint: number;
  reorderQuantity: number;
}

export interface CreateCategoryPayload {
  name: string;
  parentId: string | null;
}

export interface CreateSkuPayload {
  skuCode: string;
  name: string;
  categoryId: string | null;
  weightKg: number;
  dimensions: SkuDimensions;
  unit: string;
  reorderPoint: number;
  reorderQuantity: number;
}