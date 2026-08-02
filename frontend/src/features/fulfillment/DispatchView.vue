<!--
  Manager oversight view for dispatched shipments.
  Flat table, read-only. Distinct from the dispatcher worker terminal
  (features/dispatcher/DispatchDeskView.vue), which is the active
  carrier-assignment queue — this view is the historical/status record.
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useDispatchOversightStore } from '@/stores/dispatchOversightStore';
import type { ShipmentStatus } from '@/core/models/shipment'; 

const store = useDispatchOversightStore();
const statusFilter = ref<ShipmentStatus | 'ALL'>('ALL');

async function applyFilter() {
  await store.fetchAll(statusFilter.value === 'ALL' ? undefined : statusFilter.value);
}

/** Format an ISO timestamp for display, or an em dash if not yet set. */
function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString();
}

onMounted(applyFilter);
</script>

<template>
  <div class="dispatch-view">
    <div class="dispatch-view__toolbar flex-row">
      <select v-model="statusFilter" @change="applyFilter" class="dispatch-view__filter">
        <option value="ALL">All statuses</option>
        <option value="PENDING">Pending</option>
        <option value="DISPATCHED">Dispatched</option>
        <option value="DELIVERED">Delivered</option>
      </select>
    </div>

    <p v-if="store.loading" class="dispatch-view__meta">Loading…</p>
    <p v-else-if="store.error" class="dispatch-view__meta dispatch-view__meta--error">{{ store.error }}</p>
    <p v-else-if="store.shipments.length === 0" class="dispatch-view__meta">No shipments found.</p>

    <table v-else class="dispatch-view__table">
      <thead>
        <tr>
          <th>Parcel</th>
          <th>Carrier</th>
          <th>Carrier tracking #</th>
          <th>Dispatched by</th>
          <th>Dispatched at</th>
          <th>Est. delivery</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in store.shipments" :key="s.id">
          <td class="dispatch-view__mono">{{ s.parcelTrackingNumber }}</td>
          <td>{{ s.carrierName }}</td>
          <td class="dispatch-view__mono">{{ s.carrierTrackingNumber }}</td>
          <td>{{ s.dispatchedBy?.employeeId ?? '—' }}</td>
          <td>{{ formatDate(s.dispatchedAt) }}</td>
          <td>{{ formatDate(s.estimatedDelivery) }}</td>
          <td class="dispatch-view__mono">{{ s.status }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style lang="scss" scoped>
.flex-row { display: flex; align-items: center; gap: 0.75rem; }

.dispatch-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  &__toolbar {
    justify-content: flex-start;
  }

  &__filter {
    padding: 0.4rem 0.6rem;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    background: var(--surface-1);
    color: var(--text-primary);
  }

  &__meta {
    color: var(--text-secondary);
    font-size: 0.875rem;

    &--error {
      color: var(--domain-danger);
    }
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    background: var(--surface-1);
    border: 0.5px solid var(--border);
    border-radius: 8px;
    overflow: hidden;

    th, td {
      text-align: left;
      padding: 0.6rem 1rem;
      border-bottom: 0.5px solid var(--border);
      font-size: 0.875rem;
    }

    th {
      color: var(--text-secondary);
      font-weight: 600;
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    tr:last-child td {
      border-bottom: none;
    }
  }

  &__mono {
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
    font-size: 0.8125rem;
    color: var(--text-secondary);
  }
}
</style>