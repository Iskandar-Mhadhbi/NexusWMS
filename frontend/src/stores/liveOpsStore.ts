/**
 * liveOpsStore.ts
 *
 * Owns the Flow Rail's live state. Connected once from ManagerShell (on
 * app-shell mount, never per-page — see phase7_frontend_design.md §7, so
 * navigating between manager pages never drops or reopens the socket).
 *
 * Derives two things per zone from the incoming WarehouseEvent stream:
 *
 *  1. Moving dots — one per event, auto-removed after DOT_LIFETIME_MS.
 *     Direct 1:1 mapping: "an event just happened here" → "a dot drifts
 *     through this zone."
 *
 *  2. Worker presence — approximated from distinct workerIds seen per zone
 *     within a rolling ACTIVITY_WINDOW_MS window. This is a client-side
 *     proxy, not a real presence feed, since the backend has no dedicated
 *     "who's currently in this zone" endpoint — it's inferred from whoever
 *     recently triggered an event there.
 *
 * occupancyPercent is intentionally NOT derived from live data — see the
 * file-level note in the chat/design-doc: no confirmed WebSocket channel
 * streams the planned zone:occupancy:{zoneId} Redis key yet. Left as a
 * static per-zone placeholder rather than fabricating a number from event
 * frequency and presenting it as real. Tracked as an open backend item in
 * phase7_frontend_design.md.
 */
import { defineStore } from 'pinia';
import {
  connectDashboardSocket,
  disconnectDashboardSocket,
  subscribe,
} from '@/core/services/websocketService';
import type { WarehouseEvent } from '@/core/models/warehouseEvent';

/** How long a worker is still considered "on floor" after their last event. */
const ACTIVITY_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

/** How long a moving-dot animation plays before it's removed from state.
 *  Must stay in sync with the `drift` keyframe duration in FlowRail.vue. */
const DOT_LIFETIME_MS = 3500;

export type ZoneKey = 'RECEIVING' | 'STORAGE' | 'PICKING' | 'PACKING' | 'DISPATCH';
export type Domain = 'procurement' | 'inventory' | 'fulfillment' | 'dispatch';

interface MovingDot {
  id: string;
}

interface WorkerActivity {
  workerId: string;
  lastSeenAt: number;
}

interface ZoneState {
  key: ZoneKey;
  label: string;
  domain: Domain;
  /** Static placeholder — see file header. */
  occupancyPercent: number;
  dots: MovingDot[];
  workerActivity: WorkerActivity[];
}

/** Maps a backend zone label to one of the Flow Rail's five physical zones.
 *  Events tagged with a non-physical label (e.g. "OMS" for order creation,
 *  which isn't tied to a shelf/zone yet) simply have no entry here and are
 *  ignored by the rail — there's nothing to draw for them. */
const ZONE_LOOKUP: Record<string, ZoneKey> = {
  RECEIVING: 'RECEIVING',
  STORAGE: 'STORAGE',
  'STORAGE-A': 'STORAGE',
  'STORAGE-B': 'STORAGE',
  PICKING: 'PICKING',
  PACKING: 'PACKING',
  DISPATCH: 'DISPATCH',
};

export const useLiveOpsStore = defineStore('liveOps', {
  state: () => ({
    connected: false,
    zones: [
      { key: 'RECEIVING', label: 'Receiving', domain: 'procurement', occupancyPercent: 60, dots: [], workerActivity: [] },
      { key: 'STORAGE', label: 'Storage', domain: 'inventory', occupancyPercent: 40, dots: [], workerActivity: [] },
      { key: 'PICKING', label: 'Picking', domain: 'fulfillment', occupancyPercent: 85, dots: [], workerActivity: [] },
      { key: 'PACKING', label: 'Packing', domain: 'fulfillment', occupancyPercent: 55, dots: [], workerActivity: [] },
      { key: 'DISPATCH', label: 'Dispatch', domain: 'dispatch', occupancyPercent: 20, dots: [], workerActivity: [] },
    ] as ZoneState[],
  }),

  getters: {
    /** @returns count of workers active in `zoneKey` within the rolling window */
    workerCount: (state) => (zoneKey: ZoneKey): number => {
      const zone = state.zones.find((z) => z.key === zoneKey);
      if (!zone) return 0;
      const cutoff = Date.now() - ACTIVITY_WINDOW_MS;
      return zone.workerActivity.filter((w) => w.lastSeenAt >= cutoff).length;
    },
  },

  actions: {
    /** Opens the socket and subscribes to both broadcast channels. Call once. */
    connect() {
      if (this.connected) return;
      connectDashboardSocket(() => {
        this.connected = true;
        subscribe('/topic/dashboard', (payload) => this.handleEvent(payload as WarehouseEvent));
        subscribe('/topic/alerts', (payload) => this.handleEvent(payload as WarehouseEvent));
      });
    },

    /** Closes the socket and clears connection state. */
    disconnect() {
      disconnectDashboardSocket();
      this.connected = false;
    },

    /**
     * Routes an incoming WarehouseEvent to its zone: spawns a moving dot
     * and records the acting worker for presence tracking.
     */
    handleEvent(event: WarehouseEvent) {
      const zoneKey = ZONE_LOOKUP[event.zone];
      if (!zoneKey) return;

      const zone = this.zones.find((z) => z.key === zoneKey);
      if (!zone) return;

      const dotId = `${event.referenceId}-${Date.now()}`;
      zone.dots.push({ id: dotId });
      setTimeout(() => {
        zone.dots = zone.dots.filter((d) => d.id !== dotId);
      }, DOT_LIFETIME_MS);

      if (event.workerId) {
        const existing = zone.workerActivity.find((w) => w.workerId === event.workerId);
        if (existing) {
          existing.lastSeenAt = Date.now();
        } else {
          zone.workerActivity.push({ workerId: event.workerId, lastSeenAt: Date.now() });
        }
      }
    },
  },
});