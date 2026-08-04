<!--
  Manager oversight view for pick lists.
  Lists all pick lists with status filter; expand a row to see its items
  (shelf, SKU, quantity, per-item status). No create/edit here — pick lists
  are generated from Orders → "generate fulfillment request" → pick-list
  generation, not built manually.
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { usePickListOversightStore } from '@/stores/pickListOversightStore';
import type { PickListStatus } from '@/core/models/pickList'; 

const store = usePickListOversightStore();
const statusFilter = ref<PickListStatus | 'ALL'>('ALL');
const expandedId = ref<string | null>(null);

/** Reload the list whenever the status filter changes. Store already surfaces failures via store.error for display — swallow the re-thrown rejection here since this component has no further action to take on failure. */
async function applyFilter() {
  try {
    await store.fetchAll(statusFilter.value === 'ALL' ? undefined : statusFilter.value);
  } catch {
    // Intentionally empty — store.error is already set and rendered.
  }
}

/** Toggle a row's item detail open/closed. */
function toggleExpand(id: string) {
  expandedId.value = expandedId.value === id ? null : id;
}

onMounted(applyFilter);
</script>

<template>
  <div class="pick-lists-view">
    <div class="pick-lists-view__toolbar flex-row">
      <select v-model="statusFilter" @change="applyFilter" class="pick-lists-view__filter">
        <option value="ALL">All statuses</option>
        <option value="GENERATED">Generated</option>
        <option value="IN_PROGRESS">In progress</option>
        <option value="COMPLETED">Completed</option>
      </select>
    </div>

    <p v-if="store.loading" class="pick-lists-view__meta">Loading…</p>
    <p v-else-if="store.error" class="pick-lists-view__meta pick-lists-view__meta--error">{{ store.error }}</p>
    <p v-else-if="store.pickLists.length === 0" class="pick-lists-view__meta">No pick lists found.</p>

    <div
      v-for="pl in store.pickLists"
      :key="pl.id"
      class="pick-lists-view__row"
    >
      <div class="pick-lists-view__row-header flex-row" @click="toggleExpand(pl.id)">
        <span class="pick-lists-view__id">{{ pl.id.slice(0, 8) }}</span>
        <span class="pick-lists-view__assigned">{{ pl.assignedTo.employeeId }}</span>
        <span class="pick-lists-view__generated-by" v-if="pl.generatedBy">by {{ pl.generatedBy.employeeId }}</span>
        <span class="pick-lists-view__count">{{ pl.items.length }} items</span>
      </div>

      <div v-if="expandedId === pl.id" class="pick-lists-view__items flex-column">
        <div
          v-for="item in pl.items"
          :key="item.id"
          class="pick-lists-view__item flex-row"
        >
          <span class="pick-lists-view__shelf">{{ item.shelfCode }}</span>
          <span class="pick-lists-view__sku">{{ item.skuCode }}</span>
          <span class="pick-lists-view__qty">{{ item.quantityPicked }} / {{ item.quantityToPick }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
// Flex-only layout per project convention (no CSS grid).
.flex-row { display: flex; align-items: center; gap: 0.75rem; }
.flex-column { display: flex; flex-direction: column; gap: 0.5rem; }

.pick-lists-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  &__toolbar {
    justify-content: flex-start;
  }

  &__generated-by {
  font-size: 0.75rem;
  color: var(--text-muted);
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

  &__row {
    border: 0.5px solid var(--border);
    border-radius: 8px;
    background: var(--surface-1);
    overflow: hidden;
  }

  &__row-header {
    padding: 0.75rem 1rem;
    cursor: pointer;
    justify-content: flex-start;

    &:hover {
      background: color-mix(in srgb, var(--domain-fulfillment) 6%, var(--surface-1));
    }
  }

  &__id,
  &__assigned {
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
    font-size: 0.8125rem;
    color: var(--text-secondary);
  }

  &__count {
    margin-left: auto;
    font-size: 0.75rem;
    color: var(--text-muted);
  }

  &__items {
    padding: 0.5rem 1rem 1rem 2rem;
    border-top: 0.5px solid var(--border);
  }

  &__item {
    justify-content: flex-start;
    font-size: 0.875rem;
  }

  &__shelf,
  &__sku {
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
    font-size: 0.8125rem;
  }

  &__qty {
    color: var(--text-secondary);
  }
}
</style>