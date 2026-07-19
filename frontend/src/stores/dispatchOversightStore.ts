/**
 * Manager oversight state for shipments (Fulfillment pillar).
 * Read-only — shipments are created via the dispatcher worker terminal's
 * dispatch action, not directly from this view.
 */

import { defineStore } from 'pinia';
import { ref } from 'vue';
import { dispatchOversightService } from '@/features/fulfillment/services/dispatchOversightService';
import type { ShipmentResponse, ShipmentStatus } from '@/core/models/shipment';

export const useDispatchOversightStore = defineStore('dispatchOversight', () => {
  const shipments = ref<ShipmentResponse[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchAll(status?: ShipmentStatus) {
    loading.value = true;
    error.value = null;
    try {
      shipments.value = await dispatchOversightService.getAll(status);
    } catch (e) {
      error.value = 'Failed to load shipments.';
      throw e;
    } finally {
      loading.value = false;
    }
  }

  return { shipments, loading, error, fetchAll };
});