<!--
  Manager oversight view for packing tasks.
  Flat list (no expand/detail needed — packing tasks don't carry a nested
  item list the way pick lists do; the interesting detail is status +
  actor + timestamps). Read-only, no actions here.
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { usePackingOversightStore } from '@/stores/packingOversightStore';
import type { PackingTaskStatus } from '@/core/models/packingTask'; 

const store = usePackingOversightStore();
const statusFilter = ref<PackingTaskStatus | 'ALL'>('ALL');

async function applyFilter() {
  await store.fetchAll(statusFilter.value === 'ALL' ? undefined : statusFilter.value);
}

onMounted(applyFilter);
</script>

<template>
  <div class="packing-view">
    <div class="packing-view__toolbar flex-row">
      <select v-model="statusFilter" @change="applyFilter" class="packing-view__filter">
        <option value="ALL">All statuses</option>
        <option value="PENDING">Pending</option>
        <option value="IN_PROGRESS">In progress</option>
        <option value="COMPLETED">Completed</option>
      </select>
    </div>

    <p v-if="store.loading" class="packing-view__meta">Loading…</p>
    <p v-else-if="store.error" class="packing-view__meta packing-view__meta--error">{{ store.error }}</p>
    <p v-else-if="store.tasks.length === 0" class="packing-view__meta">No packing tasks found.</p>

    <table v-else class="packing-view__table">
      <thead>
        <tr>
          <th>Task #</th>
          <th>Pick list</th>
          <th>Assigned</th>
          <th>Started by</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="task in store.tasks" :key="task.id">
          <td class="packing-view__mono">{{ task.taskNumber }}</td>
          <td class="packing-view__mono">{{ task.pickListId.slice(0, 8) }}</td>
          <td>{{ task.assignedTo.employeeId }}</td>
          <td>{{ task.startedBy?.employeeId ?? '—' }}</td>
          <td class="packing-view__mono">{{ task.status }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style lang="scss" scoped>
.flex-row { display: flex; align-items: center; gap: 0.75rem; }

.packing-view {
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