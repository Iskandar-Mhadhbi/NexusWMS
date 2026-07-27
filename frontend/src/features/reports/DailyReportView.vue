<!--
  Daily report — Reports pillar.
  Worker performance is scoped by a single date (real backend param, no
  range/trend yet). Stock valuation is always a current snapshot, shown
  separately since it isn't date-scoped the way worker performance is.
-->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useDailyReportStore } from '@/stores/dailyReportStore';

const store = useDailyReportStore();

const today = new Date().toISOString().slice(0, 10);
const selectedDate = ref(today);

async function loadForDate() {
  await store.fetchAll(selectedDate.value);
}

const rankedPickers = computed(() => {
  const list = store.workerPerformance?.pickers ?? [];
  return [...list].sort((a, b) => b.items_picked - a.items_picked);
});
const rankedPackers = computed(() => {
  const list = store.workerPerformance?.packers ?? [];
  return [...list].sort((a, b) => b.tasks_completed - a.tasks_completed);
});
const maxPicked = computed(() => rankedPickers.value[0]?.items_picked ?? 1);
const maxPacked = computed(() => rankedPackers.value[0]?.tasks_completed ?? 1);

const topSkusByValue = computed(() => {
  const list = store.stockValuation?.skus ?? [];
  return [...list].sort((a, b) => b.total_value - a.total_value).slice(0, 8);
});
const maxSkuValue = computed(() => topSkusByValue.value[0]?.total_value ?? 1);

onMounted(loadForDate);
</script>

<template>
  <div class="daily-report">
    <section class="daily-report__section">
      <div class="daily-report__section-header flex-row">
        <h3 class="daily-report__section-title">Worker performance</h3>
        <input type="date" v-model="selectedDate" @change="loadForDate" class="daily-report__date-input" />
      </div>

      <p v-if="store.loading" class="daily-report__meta">Loading…</p>
      <p v-else-if="store.error" class="daily-report__meta daily-report__meta--error">{{ store.error }}</p>

      <div v-else class="daily-report__split flex-row">
        <div class="daily-report__panel flex-column">
          <p class="daily-report__panel-label">Pickers</p>
          <p v-if="rankedPickers.length === 0" class="daily-report__meta">No picking activity on this date.</p>
          <div v-for="p in rankedPickers" :key="p.worker_id" class="daily-report__bar-row flex-column">
            <div class="daily-report__bar-row-top flex-row">
              <!-- TODO(backend): raw UUID, no name resolution available yet -->
              <span class="daily-report__mono">{{ p.worker_id.slice(0, 8) }}</span>
              <span class="daily-report__value">{{ p.items_picked }} picked</span>
            </div>
            <div class="daily-report__bar-track">
              <div class="daily-report__bar-fill" :style="{ width: (p.items_picked / maxPicked * 100) + '%' }" />
            </div>
          </div>
        </div>

        <div class="daily-report__panel flex-column">
          <p class="daily-report__panel-label">Packers</p>
          <p v-if="rankedPackers.length === 0" class="daily-report__meta">No packing activity on this date.</p>
          <div v-for="p in rankedPackers" :key="p.worker_id" class="daily-report__bar-row flex-column">
            <div class="daily-report__bar-row-top flex-row">
              <span class="daily-report__mono">{{ p.worker_id.slice(0, 8) }}</span>
              <span class="daily-report__value">{{ p.tasks_completed }} packed</span>
            </div>
            <div class="daily-report__bar-track">
              <div class="daily-report__bar-fill" :style="{ width: (p.tasks_completed / maxPacked * 100) + '%' }" />
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="daily-report__section">
      <div class="daily-report__section-header flex-row">
        <h3 class="daily-report__section-title">Stock valuation</h3>
        <span class="daily-report__snapshot-note">current snapshot — not date-scoped</span>
      </div>

      <div v-if="store.stockValuation" class="daily-report__valuation-headline">
        {{ store.stockValuation.summary.total_inventory_value.toLocaleString(undefined, { style: 'currency', currency: 'USD' }) }}
      </div>

      <div class="daily-report__panel flex-column">
        <p class="daily-report__panel-label">Top SKUs by value</p>
        <div v-for="s in topSkusByValue" :key="s.sku_id" class="daily-report__bar-row flex-column">
          <div class="daily-report__bar-row-top flex-row">
            <span class="daily-report__mono">{{ s.sku_code }}</span>
            <span class="daily-report__value">{{ s.total_value.toLocaleString(undefined, { style: 'currency', currency: 'USD' }) }}</span>
          </div>
          <div class="daily-report__bar-track">
            <div class="daily-report__bar-fill" :style="{ width: (s.total_value / maxSkuValue * 100) + '%' }" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style lang="scss" scoped>
.flex-row { display: flex; align-items: center; gap: 0.75rem; }
.flex-column { display: flex; flex-direction: column; gap: 0.5rem; }

.daily-report {
  display: flex;
  flex-direction: column;
  gap: 2rem;

  &__section-header { justify-content: space-between; }
  &__section-title { font-size: 1.0625rem; font-weight: 600; margin: 0; }

  &__date-input {
    padding: 0.35rem 0.6rem;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    background: var(--surface-1);
    color: var(--text-primary);
    font-size: 0.8125rem;
  }

  &__snapshot-note { font-size: 0.75rem; color: var(--text-muted); }

  &__meta {
    color: var(--text-secondary);
    font-size: 0.875rem;
    &--error { color: var(--domain-danger); }
  }

  &__split { align-items: flex-start; flex-wrap: wrap; }

  &__panel {
    flex: 1;
    min-width: 260px;
    background: var(--surface-1);
    border: 0.5px solid var(--border);
    border-radius: 10px;
    padding: 1rem 1.25rem;
    gap: 0.75rem;
  }

  &__panel-label {
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    color: var(--text-secondary);
    margin: 0;
  }

  &__bar-row-top { justify-content: space-between; font-size: 0.8125rem; }
  &__mono { font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace; color: var(--text-secondary); }
  &__value { color: var(--text-primary); font-weight: 500; }

  &__bar-track {
    height: 6px;
    background: var(--border);
    border-radius: 3px;
    overflow: hidden;
  }

  &__bar-fill {
    height: 100%;
    background: var(--domain-reports, #2C8C89);
    border-radius: 3px;
  }

  &__valuation-headline {
    font-size: 1.75rem;
    font-weight: 600;
    color: var(--domain-reports, #2C8C89);
  }
}
</style>