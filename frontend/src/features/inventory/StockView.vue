<!--
  StockView.vue
  Inventory → Stock tab. Read-only pass: stock levels table + open reorder
  alerts. Adjustment (POST /stock/adjust) is deliberately deferred until a
  real shelf picker exists — this view has no write actions.

  Row click expands an inline panel with the SKU's per-shelf/batch location
  breakdown, cached in stockStore after first fetch. No modal/drawer
  component exists yet in this codebase, so this uses a plain expand-in-
  place row rather than introducing new shared UI unprompted.

  Styling matches SkusView.vue conventions exactly: no flex utility
  classes (display/flex-direction/etc. written out per-element in SCSS),
  var(--domain-inventory) / var(--domain-danger) for accents, 0.5px
  borders, unitless px sizes.
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useStockStore } from '@/stores/stockStore';

const store = useStockStore();

/** skuId of the currently expanded row, or null if none expanded. */
const expandedSkuId = ref<string | null>(null);

onMounted(() => {
  store.fetchLevels();
  store.fetchReorderAlerts();
});

/** Toggle a row's expanded state; lazy-loads its locations on first open. */
function toggleRow(skuId: string) {
  if (expandedSkuId.value === skuId) {
    expandedSkuId.value = null;
    return;
  }
  expandedSkuId.value = skuId;
  store.fetchLocations(skuId);
}
</script>

<template>
  <div class="stock">
    <div class="stock__header">
      <h1 class="stock__title">Stock Levels</h1>
      <span v-if="store.needsReorderCount > 0" class="stock__reorder-count">
        {{ store.needsReorderCount }} SKU{{ store.needsReorderCount > 1 ? 's' : '' }} need reorder
      </span>
    </div>

    <!-- Reorder alerts -->
    <div v-if="store.reorderAlertsEnriched.length > 0" class="reorder-alerts">
      <h2 class="reorder-alerts__title">Open Reorder Alerts</h2>
      <div class="reorder-alerts__list">
        <div v-for="alert in store.reorderAlertsEnriched" :key="alert.id" class="reorder-alerts__row">
          <span class="reorder-alerts__sku">{{ alert.skuCode ?? alert.skuId }}</span>
          <span v-if="alert.skuName" class="reorder-alerts__name">{{ alert.skuName }}</span>
          <span class="reorder-alerts__qty">{{ alert.currentQuantity }} / {{ alert.reorderPoint }}</span>
        </div>
      </div>
    </div>

    <p v-if="store.error" class="stock__error">{{ store.error }}</p>

    <p v-if="store.loadingLevels" class="stock__meta">Loading stock levels…</p>

    <div v-else class="stock__list">
      <div class="stock-head">
        <span class="stock-head__col stock-head__col--sku">SKU</span>
        <span class="stock-head__col stock-head__col--name">Name</span>
        <span class="stock-head__col stock-head__col--num">Total</span>
        <span class="stock-head__col stock-head__col--num">Reserved</span>
        <span class="stock-head__col stock-head__col--num">Available</span>
        <span class="stock-head__col stock-head__col--num">Reorder Pt.</span>
        <span class="stock-head__col stock-head__col--status">Status</span>
      </div>

      <template v-for="row in store.levels" :key="row.skuId">
        <div class="stock-row" :class="{ 'stock-row--needs-reorder': row.needsReorder }" @click="toggleRow(row.skuId)">
          <span class="stock-row__code">{{ row.skuCode }}</span>
          <span class="stock-row__name">{{ row.skuName }}</span>
          <span class="stock-row__num">{{ row.totalQuantity }}</span>
          <span class="stock-row__num">{{ row.totalReserved }}</span>
          <span class="stock-row__num">{{ row.availableQuantity }}</span>
          <span class="stock-row__num">{{ row.reorderPoint }}</span>
          <span class="stock-row__status">
            <span v-if="row.needsReorder" class="stock-row__badge stock-row__badge--danger">Reorder</span>
            <span v-else class="stock-row__badge stock-row__badge--ok">OK</span>
          </span>
        </div>

        <!-- Expanded location breakdown -->
        <div v-if="expandedSkuId === row.skuId" class="location-panel">
          <p v-if="store.loadingLocations && !store.locationsBySku[row.skuId]" class="location-panel__meta">
            Loading locations…
          </p>
          <div v-for="loc in store.locationsBySku[row.skuId] ?? []" :key="loc.id" class="location-panel__row">
            <span class="location-panel__shelf">{{ loc.shelfCode ?? loc.shelfId }}</span>
            <span class="location-panel__qty">Qty {{ loc.quantity }} (reserved {{ loc.reservedQuantity }})</span>
            <span v-if="loc.batchId" class="location-panel__batch">{{ loc.batchId }}</span>
            <span v-if="loc.expiryDate" class="location-panel__expiry">exp. {{ loc.expiryDate }}</span>
          </div>
          <p
            v-if="(store.locationsBySku[row.skuId] ?? []).length === 0 && !store.loadingLocations"
            class="location-panel__meta"
          >
            No locations found for this SKU.
          </p>
        </div>
      </template>

      <p v-if="store.levels.length === 0" class="stock__meta">No stock records found.</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.stock {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  flex: 1;
}

.stock__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.stock__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.stock__reorder-count {
  font-size: 13px;
  font-weight: 500;
  color: var(--domain-danger);
  background: color-mix(in srgb, var(--domain-danger) 10%, transparent);
  padding: 4px 10px;
  border-radius: 999px;
}

.stock__error {
  color: var(--domain-danger);
  font-size: 13px;
}

.stock__meta {
  color: var(--text-muted);
  font-size: 14px;
}

.reorder-alerts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 1rem;
  border: 0.5px solid var(--domain-danger);
  border-radius: 10px;
  background: color-mix(in srgb, var(--domain-danger) 5%, transparent);
}

.reorder-alerts__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.reorder-alerts__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reorder-alerts__row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  font-size: 13px;
}

.reorder-alerts__sku {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-weight: 500;
  color: var(--text-primary);
  min-width: 96px;
  flex-shrink: 0;
}

.reorder-alerts__name {
  flex: 1;
  color: var(--text-secondary);
}

.reorder-alerts__qty {
  color: var(--domain-danger);
  font-weight: 600;
}

.stock__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-head {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.stock-head__col {
  &--sku { flex: 0 0 100px; }
  &--name { flex: 1; }
  &--num { flex: 0 0 88px; text-align: right; }
  &--status { flex: 0 0 88px; text-align: right; }
}

.stock-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;

  &--needs-reorder {
    background: color-mix(in srgb, var(--domain-danger) 4%, var(--surface-1));
  }
}

.stock-row__code {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  color: var(--text-secondary);
  flex: 0 0 100px;
}

.stock-row__name {
  font-size: 14px;
  color: var(--text-primary);
  flex: 1;
}

.stock-row__num {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
  flex: 0 0 88px;
  text-align: right;
}

.stock-row__status {
  flex: 0 0 88px;
  text-align: right;
}

.stock-row__badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;

  &--danger {
    color: var(--domain-danger);
    background: color-mix(in srgb, var(--domain-danger) 12%, transparent);
  }
  &--ok {
    color: var(--text-secondary);
    background: color-mix(in srgb, var(--domain-inventory) 12%, transparent);
  }
}

.location-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 14px 10px 32px;
  background: var(--surface-0);
  border: 0.5px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.location-panel__meta {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0;
}

.location-panel__row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
}

.location-panel__shelf {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-weight: 500;
  color: var(--text-primary);
}

.location-panel__batch {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.location-panel__expiry {
  color: var(--text-muted);
  font-size: 12px;
}
</style>