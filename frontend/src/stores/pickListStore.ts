/**
 * pickListStore.ts
 * Owns the picker terminal's active pick list state. After a successful
 * pick, deliberately refetches the whole list via load() rather than
 * trusting the pick endpoint's response shape — that shape isn't confirmed
 * (see pickList.ts header), so re-fetching from the known-good GET is the
 * safer source of truth.
 */
import { defineStore } from 'pinia';
import { pickListService } from '@/features/picker/services/pickListService';
import type { PickList, PickListItem } from '@/core/models/pickList';

export const usePickListStore = defineStore('pickList', {
  state: () => ({
    pickList: null as PickList | null,
    loading: false,
    submitting: false,
    error: null as string | null,
  }),

  getters: {
    /** First PENDING item — the one currently shown on the terminal. */
    currentItem: (state): PickListItem | null =>
      state.pickList?.items.find((i) => i.status === 'PENDING') ?? null,

    pickedCount: (state): number =>
      state.pickList?.items.filter((i) => i.status !== 'PENDING').length ?? 0,

    totalCount: (state): number => state.pickList?.items.length ?? 0,

    isComplete: (state): boolean =>
      state.pickList != null && state.pickList.items.every((i) => i.status !== 'PENDING'),
  },

  actions: {
    async load() {
      this.loading = true;
      this.error = null;
      try {
        const { data } = await pickListService.getMyActivePickList();
        this.pickList = data;
      } catch (err) {
        this.error = 'No active pick list assigned.';
        console.error('[pickListStore] load failed:', err);
      } finally {
        this.loading = false;
      }
    },

    /** Confirms the current item, then reloads to get authoritative state. */
    async confirmCurrentItem() {
      if (!this.pickList || !this.currentItem) return;
      this.submitting = true;
      this.error = null;
      try {
        await pickListService.pickItem(this.pickList.id, this.currentItem.id, this.currentItem.quantityToPick);
        await this.load();
      } catch (err) {
        this.error = 'Could not confirm pick — try again.';
        console.error('[pickListStore] pick failed:', err);
      } finally {
        this.submitting = false;
      }
    },
  },
});