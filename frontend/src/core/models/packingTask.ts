/**
 * packingTask.ts
 * Frontend mirror of the PackingTask domain object (com.nexuswms.fulfillment)
 * — see phase4/5/8_progress.md. startedBy was added in Phase 8 alongside
 * assignedTo, to distinguish who was scheduled vs who actually did the work.
 *
 * NOTE: PackingTaskResponse's exact full field list was never fully
 * enumerated in the phase docs (only "startedBy field added" is confirmed
 * from phase8_progress.md) — fields beyond that are reasonable inference,
 * not confirmed source.
 */
export type PackingTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export interface PackingTask {
  id: string;
  pickListId: string;
  assignedTo: string;
  status: PackingTaskStatus;
  startedAt: string | null;
  startedBy: string | null;
  completedAt: string | null;
}

/**
 * Minimal shape of the Parcel created on task completion. Whether
 * POST /packing-tasks/{id}/complete actually returns this nested, returns
 * it as a flat sibling, or doesn't return it at all is unconfirmed — the
 * store handles all three gracefully (see packingTaskStore.ts).
 */
export interface ParcelSummary {
  id: string;
  trackingNumber: string;
  barcode: string;
}


export interface PackingTaskResponse {
  id: string;
  pickListId: string;
  assignedTo: string;
  startedBy: string | null;
  stationId: string | null;
  status: PackingTaskStatus;
  startedAt: string | null;
  completedAt: string | null;
  parcelId: string | null;        // set once completed
  trackingNumber: string | null;  // set once completed, if response includes nested parcel
}