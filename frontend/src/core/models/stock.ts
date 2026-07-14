/**
 * Stock domain models.
 *
 * Mirrors the response DTOs exposed by StockController
 * (com.nexuswms.inventory). Read-only shapes only for now — an
 * adjustment request/response type will be added once POST /stock/adjust
 * gets wired up (blocked on a real shelf picker from the Zones tab).
 *
 * NOTE: ReorderAlertResponse was never explicitly listed as a documented
 * DTO in prior backend notes (SkuLocationResponse / StockSummaryResponse /
 * StockAdjustmentRequest / StockMovementResponse were, ReorderAlert wasn't).
 * `ReorderAlert` below is shaped from the raw `reorder_alerts` table
 * columns as a best guess — skuCode/skuName are NOT assumed present.
 * Confirm against a real response before trusting anything beyond the
 * table's own columns.
 */

/** One row of the stock summary — GET /api/v1/stock */
export interface StockSummary {
  skuId: string;
  skuCode: string;
  skuName: string;
  totalQuantity: number;
  totalReserved: number;
  availableQuantity: number;
  reorderPoint: number;
  needsReorder: boolean;
}

/** One shelf/batch location for a SKU — GET /api/v1/stock/{skuId}/locations */
export interface SkuLocation {
  id: string;
  skuId: string;
  shelfId: string;
  /** Confirmed null in some responses (see phase3_progress.md re: GoodsReceiptLineResponse) — always guard before display. */
  shelfCode: string | null;
  quantity: number;
  reservedQuantity: number;
  batchId: string | null;
  expiryDate: string | null;
}

/**
 * An open reorder alert — GET /api/v1/stock/alerts/reorder
 * TODO: confirm real field names once tested against a live response.
 * skuCode/skuName are deliberately NOT read from here — stockStore
 * enriches alerts by cross-referencing StockSummary instead (see below).
 */
export interface ReorderAlert {
  id: string;
  skuId: string;
  currentQuantity: number;
  reorderPoint: number;
  status: string;
  createdAt: string;
}