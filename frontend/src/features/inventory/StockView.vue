<!--
  StockView.vue — Inventory → Stock tab.
  Now includes: read-only levels + reorder alerts (as before), PLUS
  stock adjustment — quick inline adjust on existing locations, and a
  cascading zone -> aisle -> shelf picker for placing stock on a shelf
  the SKU doesn't already occupy.

  Cascading picker (not a flat all-shelves list): reuses zoneStore's
  existing per-zone lazy-fetch exactly as-is. Chosen deliberately over
  eager-loading every zone on form-open, which would fire one network
  call per zone every time the form opens — fine at 6 zones, a real
  scaling problem at real warehouse size. The cascading picker only
  ever fetches one zone's data at a time, flat regardless of warehouse
  size, and needs zero new backend work.

  Both adjustment paths validate via AdjustStockSchema before sending —
  same standard established in ZonesView.vue.
-->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useStockStore } from '@/stores/stockStore';
import { useZoneStore } from '@/stores/zoneStore';
import { AdjustStockSchema, buildAdjustStockSchema } from '@/core/models/stock';

const store = useStockStore();
const zoneStore = useZoneStore();

const expandedSkuId = ref<string | null>(null);
const submitError = ref('');

/** Quick-adjust: which existing location row has its inline form open (by SkuLocation.id). */
const quickAdjustLocationId = ref<string | null>(null);
const quickAdjustForm = ref<{ quantity: number | null; reason: string }>({ quantity: null, reason: '' });

/** New-shelf placement: whether the picker+form is open for the currently expanded SKU. */
const showNewShelfForm = ref(false);
/** Cascading picker state — null until a level is chosen. */
const pickerZoneId = ref<string | null>(null);
const pickerAisleId = ref<string | null>(null);
const pickerShelfId = ref<string | null>(null);
const newShelfForm = ref<{ quantity: number | null; batchId: string; reason: string }>({
  quantity: null, batchId: '', reason: '',
});

onMounted(() => {
  store.fetchLevels();
  store.fetchReorderAlerts();
  zoneStore.fetchZones(); // needed for the picker's first level (zone select)
});

function toggleRow(skuId: string) {
  if (expandedSkuId.value === skuId) {
    expandedSkuId.value = null;
    closeAllAdjustForms();
    return;
  }
  expandedSkuId.value = skuId;
  store.fetchLocations(skuId);
  closeAllAdjustForms();
}

function closeAllAdjustForms() {
  quickAdjustLocationId.value = null;
  showNewShelfForm.value = false;
  pickerZoneId.value = null;
  pickerAisleId.value = null;
  pickerShelfId.value = null;
}

/** Opens the quick-adjust inline form for one existing location row. */
function openQuickAdjust(locationId: string) {
  quickAdjustLocationId.value = quickAdjustLocationId.value === locationId ? null : locationId;
  quickAdjustForm.value = { quantity: null, reason: '' };
  submitError.value = '';
}

// StockView.vue — handleQuickAdjust 
async function handleQuickAdjust(skuId: string, shelfId: string, batchId: string | null, currentQty: number) {
  submitError.value = '';
  const schema = buildAdjustStockSchema(currentQty);
  const result = schema.safeParse({
    skuId, shelfId, quantity: quickAdjustForm.value.quantity, batchId, reason: quickAdjustForm.value.reason,
  });
  if (!result.success) {
    submitError.value = result.error.issues[0].message;
    return;
  }
  try {
    await store.adjustStock(skuId, result.data);
    quickAdjustLocationId.value = null;
  } catch (err) {
    submitError.value = 'Could not adjust stock — the server rejected the request.';
    console.error('[StockView] quick adjust failed:', err);
  }
}

/** Picker: when a zone is chosen, fetch its detail (aisles + shelves) and reset deeper selections. */
function selectPickerZone(zoneId: string) {
  pickerZoneId.value = zoneId;
  pickerAisleId.value = null;
  pickerShelfId.value = null;
  zoneStore.fetchZoneDetail(zoneId);
}

function selectPickerAisle(aisleId: string) {
  pickerAisleId.value = aisleId;
  pickerShelfId.value = null;
}

/** Shelves for the currently picked zone+aisle, via zoneStore's existing grouping getter. */
const pickerShelves = computed(() => {
  if (!pickerZoneId.value || !pickerAisleId.value) return [];
  return zoneStore.shelvesByAisle(pickerZoneId.value)[pickerAisleId.value] ?? [];
});

/** Submits stock placement onto a shelf chosen via the cascading picker. */
async function handleNewShelfPlacement(skuId: string) {
  submitError.value = '';
  if (!pickerShelfId.value) {
    submitError.value = 'Select a shelf first';
    return;
  }
  const result = AdjustStockSchema.safeParse({
    skuId,
    shelfId: pickerShelfId.value,
    quantity: newShelfForm.value.quantity,
    batchId: newShelfForm.value.batchId.trim() || null,
    reason: newShelfForm.value.reason,
  });
  if (!result.success) {
    submitError.value = result.error.issues[0].message;
    return;
  }
  try {
    await store.adjustStock(skuId, result.data);
    showNewShelfForm.value = false;
    pickerZoneId.value = null;
    pickerAisleId.value = null;
    pickerShelfId.value = null;
    newShelfForm.value = { quantity: null, batchId: '', reason: '' };
  } catch (err) {
    submitError.value = 'Could not place stock — the server rejected the request.';
    console.error('[StockView] new shelf placement failed:', err);
  }
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

    <p v-if="submitError" class="stock__error">{{ submitError }}</p>
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

        <div v-if="expandedSkuId === row.skuId" class="location-panel">
          <p v-if="store.loadingLocations && !store.locationsBySku[row.skuId]" class="location-panel__meta">
            Loading locations…
          </p>

          <div
            v-for="loc in store.locationsBySku[row.skuId] ?? []"
            :key="loc.id"
            class="location-panel__row"
          >
            <span class="location-panel__shelf">{{ loc.shelfCode ?? loc.shelfId }}</span>
            <span class="location-panel__qty">Qty {{ loc.quantity }} (reserved {{ loc.reservedQuantity }})</span>
            <span v-if="loc.batchId" class="location-panel__batch">{{ loc.batchId }}</span>
            <span v-if="loc.expiryDate" class="location-panel__expiry">exp. {{ loc.expiryDate }}</span>
            <button class="location-panel__adjust-btn" @click.stop="openQuickAdjust(loc.id)">
              {{ quickAdjustLocationId === loc.id ? 'Cancel' : 'Adjust' }}
            </button>
          </div>

          <form
            v-if="quickAdjustLocationId"
            class="adjust-form" 
              @submit.prevent="
                handleQuickAdjust(
                  row.skuId,
                  (store.locationsBySku[row.skuId] ?? []).find((l) => l.id === quickAdjustLocationId)!.shelfId,
                  (store.locationsBySku[row.skuId] ?? []).find((l) => l.id === quickAdjustLocationId)!.batchId,
                  (store.locationsBySku[row.skuId] ?? []).find((l) => l.id === quickAdjustLocationId)!.quantity
                )
              "
          >
            <input
              v-model.number="quickAdjustForm.quantity"
              type="number"
              :placeholder="`Quantity (+/-, max removal: ${(store.locationsBySku[row.skuId] ?? []).find((l) => l.id === quickAdjustLocationId)?.quantity ?? 0})`"
            />
            <input v-model="quickAdjustForm.reason" placeholder="Reason (e.g. damage, restock, count correction)" />
            <button type="submit">Confirm adjustment</button>
          </form>

          <p v-if="(store.locationsBySku[row.skuId] ?? []).length === 0 && !store.loadingLocations" class="location-panel__meta">
            No locations found for this SKU.
          </p>

          <button class="location-panel__new-shelf-btn" @click.stop="showNewShelfForm = !showNewShelfForm">
            {{ showNewShelfForm ? 'Cancel' : '+ Place stock on a new shelf' }}
          </button>

          <div v-if="showNewShelfForm" class="new-shelf-picker">
            <div class="new-shelf-picker__level">
              <label class="new-shelf-picker__label">Zone</label>
              <select :value="pickerZoneId" @change="selectPickerZone(($event.target as HTMLSelectElement).value)">
                <option value="" disabled selected>Select a zone…</option>
                <option v-for="z in zoneStore.zones" :key="z.id" :value="z.id">{{ z.name }}</option>
              </select>
            </div>

            <div v-if="pickerZoneId" class="new-shelf-picker__level">
              <label class="new-shelf-picker__label">Aisle</label>
              <p v-if="zoneStore.loadingAisles" class="location-panel__meta">Loading aisles…</p>
              <select v-else :value="pickerAisleId" @change="selectPickerAisle(($event.target as HTMLSelectElement).value)">
                <option value="" disabled selected>Select an aisle…</option>
                <option v-for="a in zoneStore.aislesByZone[pickerZoneId] ?? []" :key="a.id" :value="a.id">{{ a.code }}</option>
              </select>
            </div>

            <div v-if="pickerAisleId" class="new-shelf-picker__level">
              <label class="new-shelf-picker__label">Shelf</label>
              <select v-model="pickerShelfId">
                <option value="" disabled selected>Select a shelf…</option>
                <option v-for="s in pickerShelves" :key="s.id" :value="s.id">
                  {{ s.code }} (level {{ s.level }}, {{ s.currentWeight }}/{{ s.maxWeight ?? '∞' }} kg)
                </option>
              </select>
            </div>

            <form v-if="pickerShelfId" class="adjust-form" @submit.prevent="handleNewShelfPlacement(row.skuId)">
              <input v-model.number="newShelfForm.quantity" type="number" min="1" placeholder="Quantity to place" />
              <input v-model="newShelfForm.batchId" placeholder="Batch ID (optional)" />
              <input v-model="newShelfForm.reason" placeholder="Reason (e.g. goods receipt correction, restock)" />
              <button type="submit">Confirm placement</button>
            </form>
          </div>
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
  gap: 8px;
  padding: 10px 14px 12px 32px;
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

.location-panel__adjust-btn {
  margin-left: auto;
  background: none;
  border: 0.5px solid var(--border);
  color: var(--text-secondary);
  border-radius: 6px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
}

.location-panel__new-shelf-btn {
  align-self: flex-start;
  background: none;
  border: 0.5px dashed var(--domain-inventory);
  color: var(--domain-inventory);
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.adjust-form {
  display: flex;
  flex-direction: row;
  gap: 8px;
  padding: 8px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;

  input {
    flex: 1;
    padding: 6px 8px;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    font-size: 13px;
    background: var(--surface-0);
    color: var(--text-primary);
  }

  button {
    background: var(--domain-inventory);
    color: var(--surface-1);
    border: none;
    border-radius: 6px;
    padding: 6px 12px;
    font-size: 13px;
    cursor: pointer;
    white-space: nowrap;
  }
}

.new-shelf-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;

  &__level {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-secondary);
    text-transform: uppercase;
  }

  select {
    padding: 6px 8px;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    font-size: 13px;
    background: var(--surface-0);
    color: var(--text-primary);
  }
}
</style>
