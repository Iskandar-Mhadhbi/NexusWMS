<!--
  DispatchDeskView.vue
  Worker terminal for the DISPATCHER role. Unlike Pick/Pack, dispatch has
  no single per-worker assignment — any dispatcher on duty works from a
  shared queue of PACKED parcels. Each row is its own carrier-select +
  confirm unit, matching the "parcel + carrier select + confirm dispatch"
  pattern from phase7_frontend_design.md §9.2.

  This is also the future attach point for the weigh-checkpoint mismatch
  block (§3.3) — once measured vs expected weight exists, a dispatch
  attempt with a mismatch would surface here as a blocked row requiring
  override, rather than a plain carrier-select-and-go.
-->
<script setup lang="ts">
import { onMounted, reactive } from 'vue';
import { useDispatchStore } from '@/stores/dispatchStore';
import { useAuthStore } from '@/stores/authStore';

const store = useDispatchStore();
const auth = useAuthStore();

/** Per-row selected carrier, keyed by parcelId. */
const selectedCarrier = reactive<Record<string, string>>({});

onMounted(() => store.load());

/** Dispatches one parcel using its currently selected carrier. */
function handleDispatch(parcelId: string) {
  const carrierId = selectedCarrier[parcelId];
  if (!carrierId) return;
  store.dispatch(parcelId, carrierId);
}
</script>

<template>
  <div class="dispatch-desk">
    <header class="dispatch-desk__header">
      <span class="dispatch-desk__employee">{{ auth.user?.employeeId }}</span>
      <span class="dispatch-desk__status">
        <span class="dispatch-desk__status-dot"></span>
        DISPATCH
      </span>
    </header>

    <div v-if="store.loading" class="dispatch-desk__meta">Loading queue…</div>
    <p v-else-if="store.error" class="dispatch-desk__error">{{ store.error }}</p>

    <div v-else class="dispatch-desk__queue">
      <p v-if="store.parcels.length === 0" class="dispatch-desk__meta">
        No parcels awaiting dispatch.
      </p>

      <div v-for="parcel in store.parcels" :key="parcel.id" class="parcel-card">
        <span class="parcel-card__tracking">{{ parcel.trackingNumber }}</span>
        <span class="parcel-card__barcode">{{ parcel.barcode }}</span>

        <div class="parcel-card__actions">
          <select v-model="selectedCarrier[parcel.id]">
            <option value="" disabled>Select carrier</option>
            <option v-for="c in store.carriers" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
          <button
            class="parcel-card__confirm"
            :disabled="!selectedCarrier[parcel.id] || store.dispatchingId === parcel.id"
            @click="handleDispatch(parcel.id)"
          >
            {{ store.dispatchingId === parcel.id ? 'Dispatching…' : 'Confirm dispatch' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dispatch-desk {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 20px;
  gap: 16px;
  max-width: 420px;
  margin: 0 auto;
  width: 100%;
}

.dispatch-desk__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.dispatch-desk__status {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.dispatch-desk__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--domain-dispatch);
}

.dispatch-desk__meta {
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  margin-top: 40px;
}

.dispatch-desk__error {
  color: var(--domain-danger);
  font-size: 13px;
  text-align: center;
}

.dispatch-desk__queue {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.parcel-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface-1);
  border-radius: 12px;
  padding: 16px;
}

.parcel-card__tracking {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

.parcel-card__barcode {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.parcel-card__actions {
  display: flex;
  flex-direction: row;
  gap: 8px;
  margin-top: 6px;

  select {
    flex: 1;
    padding: 10px;
    border-radius: 8px;
    border: 0.5px solid var(--border);
    background: var(--surface-0);
    color: var(--text-primary);
    font-size: 14px;
  }
}

.parcel-card__confirm {
  background: var(--domain-dispatch);
  color: var(--surface-1);
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }
}
</style>