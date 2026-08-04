/**
 * Manager oversight state for pick lists (Fulfillment pillar).
 * Read-only for now — no create/cancel actions here, since pick lists are
 * generated from a FulfillmentRequest (Orders tab), not created directly.
 */

import { defineStore } from 'pinia';
import { ref } from 'vue';
import { pickListOversightService } from '@/features/fulfillment/services/pickListOversightService';
import type { PickList, PickListStatus } from '@/core/models/pickList';

export const usePickListOversightStore = defineStore('pickListOversight', () => {
  const pickLists = ref<PickList[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  /** Fetch all pick lists, optionally filtered by status. Resets error on retry. */
  async function fetchAll(status?: PickListStatus) {
    loading.value = true;
    error.value = null;
    try {
      pickLists.value = await pickListOversightService.getAll(status);
    } catch (e) {
      error.value = 'Failed to load pick lists.';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  return { pickLists, loading, error, fetchAll };
});