/**
 * dispatchStore.ts
 * Owns the dispatch desk's parcel queue + active carrier list.
 */
import { defineStore } from 'pinia';
import { dispatchService } from '@/features/dispatcher/Services/dispatchService';
import type { Parcel, Carrier } from '@/core/models/dispatch';

export const useDispatchStore = defineStore('dispatch', {
  state: () => ({
    parcels: [] as Parcel[],
    carriers: [] as Carrier[],
    loading: false,
    dispatchingId: null as string | null,
    error: null as string | null,
  }),

  actions: {
    async load() {
      this.loading = true;
      this.error = null;
      try {
        const [parcelsRes, carriersRes] = await Promise.all([
          dispatchService.listPendingParcels(),
          dispatchService.listActiveCarriers(),
        ]);
        this.parcels = parcelsRes.data;
        this.carriers = carriersRes.data;
      } catch (err) {
        this.error = 'Could not load dispatch queue.';
        console.error('[dispatchStore] load failed:', err);
      } finally {
        this.loading = false;
      }
    },

    /** Dispatches one parcel, removes it from the local queue on success. */
    async dispatch(parcelId: string, carrierId: string) {
      this.dispatchingId = parcelId;
      this.error = null;
      try {
        await dispatchService.dispatch(parcelId, carrierId);
        this.parcels = this.parcels.filter((p) => p.id !== parcelId);
      } catch (err) {
        this.error = 'Dispatch failed — try again.';
        console.error('[dispatchStore] dispatch failed:', err);
      } finally {
        this.dispatchingId = null;
      }
    },
  },
});