// ---------- Demand forecast ----------
// CONFIRMED against a real response — see forecastLine shape below.
// forecast[] is a day-by-day series (length = days_ahead), not a single
// flat number. data_points/trend_slope/message are real fields the
// backend uses to communicate forecast confidence — surfaced in the UI
// rather than hidden, since a forecast built on 3 data points shouldn't
// read with the same confidence as one built on 300.

export interface DemandForecastDay {
  day: number;
  projected_units: number;
}

export interface DemandForecastLine {
  sku_id: string;
  sku_code: string;
  name: string;
  data_points: number;
  trend_slope: number;
  forecast: DemandForecastDay[];
  message: string; // "OK" observed; other values unconfirmed (likely signals insufficient data)
}

export interface DemandForecastResponse {
  days_ahead: number;
  forecasts: DemandForecastLine[];
}

// ---------- Worker performance & stock valuation ----------
// PLACEHOLDER TYPES — not confirmed against a live response.
// Purpose: unblock compilation for dailyReportStore.ts, which was written
// against these types before the actual response shapes were verified.
// TODO: replace `Record<string, unknown>` with real field definitions
// once GET /worker/performance and GET /stock/valuation responses are
// confirmed, following the same live-verification process already used
// for DemandForecastResponse above.

export type WorkerPerformanceResponse = Record<string, unknown>;

export type StockValuationResponse = Record<string, unknown>;