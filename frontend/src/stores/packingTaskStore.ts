/**
 * packingTaskStore.ts
 * Owns the pack station terminal's active task state. Mirrors
 * pickListStore.ts's shape for consistency across worker terminals.
 */
import { defineStore } from 'pinia';
import { packingTaskService } from '@/features/packer/services/packingTaskService';
import type { PackingTask, ParcelSummary } from '@/core/models/packingTask';

export const usePackingTaskStore = defineStore('packingTask', {
  state: () => ({
    task: null as PackingTask | null,
    completedParcel: null as ParcelSummary | null,
    loading: false,
    submitting: false,
    error: null as string | null,
  }),

  actions: {
    async load() {
      this.loading = true;
      this.error = null;
      try {
        const { data } = await packingTaskService.getMyActiveTask();
        this.task = data;
      } catch (err) {
        this.error = 'No active packing task assigned.';
        console.error('[packingTaskStore] load failed:', err);
      } finally {
        this.loading = false;
      }
    },

    /** Transitions the task PENDING -> IN_PROGRESS. */
    async start() {
      if (!this.task) return;
      this.submitting = true;
      this.error = null;
      try {
        const { data } = await packingTaskService.start(this.task.id);
        this.task = data;
      } catch (err) {
        this.error = 'Could not start task — try again.';
        console.error('[packingTaskStore] start failed:', err);
      } finally {
        this.submitting = false;
      }
    },

    /**
     * Transitions the task IN_PROGRESS -> COMPLETED. Handles the Parcel
     * response defensively — see complete()'s type comment for why.
     */
    async complete() {
      if (!this.task) return;
      this.submitting = true;
      this.error = null;
      try {
        const { data } = await packingTaskService.complete(this.task.id);
        this.task = data;
        if (data.parcel) {
          this.completedParcel = data.parcel;
        }
      } catch (err) {
        this.error = 'Could not confirm pack — try again.';
        console.error('[packingTaskStore] complete failed:', err);
      } finally {
        this.submitting = false;
      }
    },
  },
});