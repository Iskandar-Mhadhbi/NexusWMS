/**
 * Manager oversight state for packing tasks (Fulfillment pillar).
 * Read-only — packing tasks are created from the Pick Lists flow
 * (PackingController POST /packing-tasks), not directly from this view.
 */

import { defineStore } from 'pinia';
import { ref } from 'vue';
import { packingOversightService } from '@/features/fulfillment/services/packingOversightService';
import type { PackingTask, PackingTaskStatus } from '@/core/models/packingTask';

export const usePackingOversightStore = defineStore('packingOversight', () => {
  const tasks = ref<PackingTask[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchAll(status?: PackingTaskStatus) {
    loading.value = true;
    error.value = null;
    try {
      tasks.value = await packingOversightService.getAll(status);
    } catch (e) {
      error.value = 'Failed to load packing tasks.';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  return { tasks, loading, error, fetchAll };
});