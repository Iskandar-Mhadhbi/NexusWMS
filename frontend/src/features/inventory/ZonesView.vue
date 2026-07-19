<!--
  ZonesView.vue
  Inventory → Zones tab. Zone cards (with occupancy fill bar) in a grid;
  clicking a card expands an aisle -> shelf list beneath the grid, other
  cards stay visible. New-zone / new-aisle / new-shelf forms follow the
  same inline-toggle pattern as SkusView.vue.

  VALIDATION STANDARD (established here, reuse for future forms):
  Each create-* handler builds a raw object from form state, runs it
  through that payload's Zod schema via safeParse(), and only calls the
  store/API if safeParse succeeds. On failure, the first issue's message
  is shown directly in submitError — no network round-trip needed to
  discover a blank required field. result.data (not the raw form ref) is
  what actually gets sent, since Zod also normalizes it (e.g. .trim()).

  shelf.maxWeight is genuinely optional — see core/models/zone.ts comment.
  TODO: occupancy fill bar uses zone.currentOccupancy / zone.capacity —
  same "static until a real channel exists" caveat as the Flow Rail
  (phase7_frontend_progress.md open items).
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useZoneStore } from '@/stores/zoneStore';
import {
  CreateZoneSchema, CreateAisleSchema, CreateShelfSchema,
  type ZoneType,
} from '@/core/models/zone';

const store = useZoneStore();

const expandedZoneId = ref<string | null>(null);
const showZoneForm = ref(false);
const showAisleForm = ref<string | null>(null); // holds zoneId while that zone's aisle form is open
const showShelfForm = ref<string | null>(null); // holds aisleId while that aisle's shelf form is open
const submitError = ref('');

/**
 * Raw form state — deliberately looser than the payload types (e.g.
 * capacity/maxWeight can transiently be NaN while a number input is
 * empty). Never sent directly; always passed through a schema first.
 */
const zoneForm = ref<{ name: string; type: ZoneType; capacity: number | null }>({
  name: '', type: 'STORAGE', capacity: null,
});
const aisleForm = ref<{ code: string }>({ code: '' });
const shelfForm = ref<{ code: string; level: string; maxWeight: number | null }>({
  code: '', level: '', maxWeight: null,
});

const zoneTypes: ZoneType[] = ['RECEIVING', 'STORAGE', 'PICKING', 'PACKING', 'DISPATCH'];

onMounted(() => store.fetchZones());

/** Toggle a zone card's expanded state; lazy-loads its aisles/shelves on first open. */
function toggleZone(zoneId: string) {
  if (expandedZoneId.value === zoneId) {
    expandedZoneId.value = null;
    return;
  }
  expandedZoneId.value = zoneId;
  store.fetchZoneDetail(zoneId);
}

function occupancyPct(occupancy: number, capacity: number): number {
  if (!capacity) return 0;
  return Math.min(100, Math.round((occupancy / capacity) * 100));
}

/** Validates zoneForm against CreateZoneSchema; only calls the store on success. */
async function handleCreateZone() {
  submitError.value = '';
  const result = CreateZoneSchema.safeParse(zoneForm.value);
  if (!result.success) {
    submitError.value = result.error.issues[0].message;
    return;
  }
  try {
    await store.createZone(result.data);
    zoneForm.value = { name: '', type: 'STORAGE', capacity: 0 };
    showZoneForm.value = false;
  } catch (err) {
    submitError.value = 'Could not create zone — the server rejected the request.';
    console.error('[ZonesView] createZone failed:', err);
  }
}

/** Validates aisleForm + zoneId against CreateAisleSchema; only calls the store on success. */
async function handleCreateAisle(zoneId: string) {
  submitError.value = '';
  const result = CreateAisleSchema.safeParse({ zoneId, code: aisleForm.value.code });
  if (!result.success) {
    submitError.value = result.error.issues[0].message;
    return;
  }
  try {
    await store.createAisle(zoneId, result.data);
    aisleForm.value = { code: '' };
    showAisleForm.value = null;
  } catch (err) {
    // A 409 here means a real, expected duplicate-code conflict within this
    // zone (V7 migration) — not a bug. Still shown as an error to the user.
    submitError.value = 'Could not create aisle — code may already exist in this zone.';
    console.error('[ZonesView] createAisle failed:', err);
  }
}

/** Validates shelfForm + aisleId against CreateShelfSchema; only calls the store on success. */
async function handleCreateShelf(zoneId: string, aisleId: string) {
  submitError.value = '';
  const result = CreateShelfSchema.safeParse({
    aisleId,
    code: shelfForm.value.code,
    level: shelfForm.value.level,
    maxWeight: shelfForm.value.maxWeight,
  });
  if (!result.success) {
    submitError.value = result.error.issues[0].message;
    return;
  }
  try {
    await store.createShelf(zoneId, result.data);
    shelfForm.value = { code: '', level: '', maxWeight: null };
    showShelfForm.value = null;
  } catch (err) {
    submitError.value = 'Could not create shelf — code may already exist in this aisle.';
    console.error('[ZonesView] createShelf failed:', err);
  }
}
</script>

<template>
  <div class="zones">
    <div class="zones__header">
      <h1 class="zones__title">Zones</h1>
      <button class="zones__new-btn" @click="showZoneForm = !showZoneForm">
        {{ showZoneForm ? 'Cancel' : 'New zone' }}
      </button>
    </div>

    <form v-if="showZoneForm" class="zones__form" @submit.prevent="handleCreateZone">
      <input v-model="zoneForm.name" placeholder="Zone name (e.g. STORAGE-A)" />
      <select v-model="zoneForm.type">
        <option v-for="t in zoneTypes" :key="t" :value="t">{{ t }}</option>
      </select>
      <input v-model.number="zoneForm.capacity" type="number" min="0" placeholder="Capacity" />
      <button type="submit" :disabled="store.creating">{{ store.creating ? 'Saving…' : 'Save zone' }}</button>
    </form>

    <p v-if="submitError" class="zones__error">{{ submitError }}</p>
    <p v-if="store.error" class="zones__error">{{ store.error }}</p>
    <p v-if="store.loadingZones" class="zones__meta">Loading zones…</p>

    <div v-else class="zones__grid">
      <div
        v-for="zone in store.zones"
        :key="zone.id"
        class="zone-card"
        :class="{ 'zone-card--expanded': expandedZoneId === zone.id }"
        @click="toggleZone(zone.id)"
      >
        <p class="zone-card__name">{{ zone.name }}</p>
        <p class="zone-card__type">type: {{ zone.type }}</p>
        <div class="zone-card__bar-track">
          <div
            class="zone-card__bar-fill"
            :style="{ width: occupancyPct(zone.currentOccupancy, zone.capacity) + '%' }"
          ></div>
        </div>
        <p class="zone-card__meta">
          {{ zone.currentOccupancy }} / {{ zone.capacity }} &middot;
          {{ occupancyPct(zone.currentOccupancy, zone.capacity) }}% occupied
        </p>
      </div>
      <p v-if="store.zones.length === 0" class="zones__meta">No zones found.</p>
    </div>

    <!-- Expanded aisle -> shelf detail for the selected zone -->
    <div v-if="expandedZoneId" class="zone-detail">
      <div class="zone-detail__header">
        <span class="zone-detail__title">
          {{ store.zones.find((z) => z.id === expandedZoneId)?.name }} &rsaquo; Aisles
        </span>
        <button class="zone-detail__new-btn" @click="showAisleForm = showAisleForm ? null : expandedZoneId">
          {{ showAisleForm === expandedZoneId ? 'Cancel' : 'New aisle' }}
        </button>
      </div>

      <form
        v-if="showAisleForm === expandedZoneId"
        class="zones__form zones__form--inline"
        @submit.prevent="handleCreateAisle(expandedZoneId)"
      >
        <input v-model="aisleForm.code" placeholder="Aisle code (e.g. A1)" />
        <button type="submit" :disabled="store.creating">{{ store.creating ? 'Saving…' : 'Save aisle' }}</button>
      </form>

      <p v-if="store.loadingAisles" class="zones__meta">Loading aisles…</p>

      <div v-else class="aisle-list">
        <template v-for="aisle in store.aislesByZone[expandedZoneId] ?? []" :key="aisle.id">
          <div class="aisle-row">
            <span class="aisle-row__code">{{ aisle.code }}</span>
            <span class="aisle-row__meta">
              {{ (store.shelvesByAisle(expandedZoneId)[aisle.id] ?? []).length }} shelves
            </span>
            <button class="aisle-row__new-btn" @click.stop="showShelfForm = showShelfForm === aisle.id ? null : aisle.id">
              {{ showShelfForm === aisle.id ? 'Cancel' : 'New shelf' }}
            </button>
          </div>

          <form
            v-if="showShelfForm === aisle.id"
            class="zones__form zones__form--nested"
            @submit.prevent="handleCreateShelf(expandedZoneId, aisle.id)"
          >
            <input v-model="shelfForm.code" placeholder="Shelf code (e.g. A1-G01)" />
            <input v-model="shelfForm.level" placeholder="Level (e.g. GROUND, G01)" />
            <input
              v-model.number="shelfForm.maxWeight"
              type="number"
              min="0"
              placeholder="Max weight (kg) — leave blank for no limit"
            />
            <button type="submit" :disabled="store.creating">{{ store.creating ? 'Saving…' : 'Save shelf' }}</button>
          </form>

          <div class="shelf-list">
            <div
              v-for="shelf in store.shelvesByAisle(expandedZoneId)[aisle.id] ?? []"
              :key="shelf.id"
              class="shelf-row"
            >
              <span class="shelf-row__code">{{ shelf.code }}</span>
              <span class="shelf-row__level">level {{ shelf.level }}</span>
              <span class="shelf-row__weight">{{ shelf.currentWeight }} / {{ shelf.maxWeight ?? '∞' }} kg</span>
            </div>
          </div>
        </template>
        <p v-if="(store.aislesByZone[expandedZoneId] ?? []).length === 0" class="zones__meta">
          No aisles found for this zone.
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.zones {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  flex: 1;
}

.zones__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.zones__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.zones__new-btn {
  background: var(--domain-inventory);
  color: var(--surface-1);
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
}

.zones__form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 10px;
  padding: 1rem;
  max-width: 360px;

  &--inline,
  &--nested {
    max-width: 320px;
  }
  &--nested {
    margin-left: 24px;
  }

  input,
  select {
    padding: 8px 10px;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    font-size: 14px;
    background: var(--surface-0);
    color: var(--text-primary);
  }

  button[type='submit'] {
    background: var(--domain-inventory);
    color: var(--surface-1);
    border: none;
    border-radius: 6px;
    padding: 8px;
    cursor: pointer;

    &:disabled {
      opacity: 0.6;
      cursor: default;
    }
  }
}

.zones__meta {
  color: var(--text-muted);
  font-size: 14px;
}

.zones__error {
  color: var(--domain-danger);
  font-size: 13px;
}

.zones__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.zone-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 12px;
  padding: 1rem;
  cursor: pointer;

  &--expanded {
    border-color: var(--domain-inventory);
  }
}

.zone-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.zone-card__type {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.zone-card__bar-track {
  height: 6px;
  border-radius: 3px;
  background: var(--surface-0);
  overflow: hidden;
}

.zone-card__bar-fill {
  height: 100%;
  background: var(--domain-inventory);
}

.zone-card__meta {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
}

.zone-detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-top: 0.5px solid var(--border);
  padding-top: 1.25rem;
}

.zone-detail__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.zone-detail__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.zone-detail__new-btn {
  background: none;
  border: 0.5px solid var(--border);
  color: var(--text-secondary);
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
}

.aisle-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.aisle-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;
}

.aisle-row__code {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  flex: 0 0 60px;
}

.aisle-row__meta {
  font-size: 13px;
  color: var(--text-secondary);
  flex: 1;
}

.aisle-row__new-btn {
  background: none;
  border: 0.5px solid var(--border);
  color: var(--text-secondary);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.shelf-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 14px 10px 32px;
}

.shelf-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  font-size: 13px;
}

.shelf-row__code {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  color: var(--text-primary);
  flex: 0 0 70px;
}

.shelf-row__level {
  color: var(--text-secondary);
  flex: 1;
}

.shelf-row__weight {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-muted);
}
</style>