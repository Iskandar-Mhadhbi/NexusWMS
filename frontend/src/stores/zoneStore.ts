/**
 * zoneStore — top-level Pinia store for the Zones tab.
 * Holds zones, plus per-zone aisle/shelf caches keyed by zoneId — since
 * a manager expands one zone at a time, aisles/shelves are lazy-loaded
 * on expand rather than eagerly fetched for every zone up front.
 *
 * Shelves are fetched at the ZONE level (GET /zones/{zoneId}/shelves),
 * not per-aisle — the backend endpoint list only exposes a zone-scoped
 * shelves route, no per-aisle one. Shelves are grouped by aisleId
 * client-side in the `shelvesByAisle` getter below. If a per-aisle
 * endpoint turns out to exist, swap this for a direct fetch instead.
 */
import { defineStore } from 'pinia';
import { zoneService } from '@/features/inventory/services/zoneService';
import type {
  Zone, Aisle, Shelf,
  CreateZonePayload, CreateAislePayload, CreateShelfPayload,
} from '@/core/models/zone';

export const useZoneStore = defineStore('zone', {
  state: () => ({
    zones: [] as Zone[],
    aislesByZone: {} as Record<string, Aisle[]>,
    shelvesByZone: {} as Record<string, Shelf[]>,
    loadingZones: false,
    loadingAisles: false,
    loadingShelves: false,
    creating: false,
    error: null as string | null,
  }),

  getters: {
    /** Shelves for a zone, grouped by aisleId. Empty array if zone/aisle has no cached shelves yet. */
    shelvesByAisle: (state) => (zoneId: string) => {
      const shelves = state.shelvesByZone[zoneId] ?? [];
      return shelves.reduce<Record<string, Shelf[]>>((acc, shelf) => {
        (acc[shelf.aisleId] ??= []).push(shelf);
        return acc;
      }, {});
    },
  },

  actions: {
    /** Fetch all zones. Call on view mount. */
    async fetchZones() {
      this.loadingZones = true;
      this.error = null;
      try {
        this.zones = await zoneService.getZones();
      } catch (err) {
        this.error = 'Failed to load zones.';
        console.error(err);
      } finally {
        this.loadingZones = false;
      }
    },

    /** Fetch (and cache) aisles + shelves for one zone. Call when a zone card is expanded. */
    async fetchZoneDetail(zoneId: string) {
      if (this.aislesByZone[zoneId] && this.shelvesByZone[zoneId]) return;
      this.loadingAisles = true;
      this.loadingShelves = true;
      try {
        const [aisles, shelves] = await Promise.all([
          zoneService.getAisles(zoneId),
          zoneService.getShelves(zoneId),
        ]);
        this.aislesByZone[zoneId] = aisles;
        this.shelvesByZone[zoneId] = shelves;
      } catch (err) {
        this.error = 'Failed to load zone detail.';
        console.error(err);
      } finally {
        this.loadingAisles = false;
        this.loadingShelves = false;
      }
    },

    /** Create a zone, then refresh the zones list. */
    async createZone(payload: CreateZonePayload) {
      this.creating = true;
      try {
        await zoneService.createZone(payload);
        await this.fetchZones();
      } finally {
        this.creating = false;
      }
    },

    /** Create an aisle under a zone, then refresh that zone's cached aisles. */
    async createAisle(zoneId: string, payload: CreateAislePayload) {
      this.creating = true;
      try {
        await zoneService.createAisle(payload);
        this.aislesByZone[zoneId] = await zoneService.getAisles(zoneId);
      } finally {
        this.creating = false;
      }
    },

    /** Create a shelf under an aisle, then refresh that zone's cached shelves. */
    async createShelf(zoneId: string, payload: CreateShelfPayload) {
      this.creating = true;
      try {
        await zoneService.createShelf(payload);
        this.shelvesByZone[zoneId] = await zoneService.getShelves(zoneId);
      } finally {
        this.creating = false;
      }
    },
  },
});