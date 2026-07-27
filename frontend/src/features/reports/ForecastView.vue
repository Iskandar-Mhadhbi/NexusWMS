<!--
  Demand forecast — Reports pillar, Forecast tab.
  Single-SKU view: a SKU must be selected before a forecast renders.
  Shows the day-by-day projected series as a sparkline, plus the
  backend's own confidence signals (data_points, trend_slope, message)
  rather than presenting a bare number as if it were certain.
-->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useForecastStore } from '@/stores/forecastStore';
import { useSkuStore } from '@/stores/skuStore';

const store = useForecastStore();
const skuStore = useSkuStore();

const daysAhead = ref(7);
const selectedSkuId = ref<string>('');

/** Only fetch once a SKU is actually selected — no meaningful "all SKUs" forecast exists. */
async function runForecast() {
  if (!selectedSkuId.value) return;
  await store.fetch(daysAhead.value, selectedSkuId.value);
}

const line = computed(() => store.forecast?.forecasts[0] ?? null);

/**
 * Trend direction label derived from trend_slope. Threshold of ±0.5 is
 * a judgment call to avoid labeling near-zero noise as "up"/"down" —
 * revisit if real slope values suggest a different scale is meaningful.
 */
const trendLabel = computed(() => {
  if (!line.value) return null;
  const slope = line.value.trend_slope;
  if (slope > 0.5) return { text: 'Trending up', symbol: '↗' };
  if (slope < -0.5) return { text: 'Trending down', symbol: '↘' };
  return { text: 'Flat', symbol: '→' };
});

/**
 * Confidence note based on data_points. Threshold of 5 is a judgment
 * call, not a backend-confirmed cutoff — the backend only gives us the
 * raw count, not its own notion of "enough."
 */
const isLowConfidence = computed(() => (line.value?.data_points ?? 0) < 5);

const maxProjected = computed(() => {
  if (!line.value || line.value.forecast.length === 0) return 1;
  return Math.max(...line.value.forecast.map((d) => d.projected_units));
});

onMounted(() => {
  skuStore.load();
});
</script>

<template>
  <div class="forecast-view">
    <div class="forecast-view__toolbar flex-row">
      <label class="forecast-view__field flex-column">
        <span>SKU</span>
        <select v-model="selectedSkuId" @change="runForecast">
          <option value="" disabled>Select a SKU…</option>
          <option v-for="sku in skuStore.skus" :key="sku.id" :value="sku.id">{{ sku.skuCode }} — {{ sku.name }}</option>
        </select>
      </label>

      <label class="forecast-view__field flex-column">
        <span>Days ahead</span>
        <input type="number" v-model.number="daysAhead" min="1" max="90" @change="runForecast" />
      </label>
    </div>

    <p v-if="!selectedSkuId" class="forecast-view__meta">Select a SKU to view its demand forecast.</p>
    <p v-else-if="store.loading" class="forecast-view__meta">Loading…</p>
    <p v-else-if="store.error" class="forecast-view__meta forecast-view__meta--error">{{ store.error }}</p>

    <div v-else-if="line" class="forecast-view__card">
      <div class="forecast-view__card-header flex-row">
        <div class="flex-column">
          <span class="forecast-view__sku-code">{{ line.sku_code }}</span>
          <span class="forecast-view__sku-name">{{ line.name }}</span>
        </div>
        <div v-if="trendLabel" class="forecast-view__trend">
          <span>{{ trendLabel.symbol }}</span> {{ trendLabel.text }}
        </div>
      </div>

      <p v-if="line.message !== 'OK'" class="forecast-view__backend-message">{{ line.message }}</p>

      <p class="forecast-view__confidence" :class="{ 'forecast-view__confidence--low': isLowConfidence }">
        Based on {{ line.data_points }} data point{{ line.data_points === 1 ? '' : 's' }}
        <span v-if="isLowConfidence">— low confidence</span>
      </p>

      <div class="forecast-view__sparkline flex-row">
        <div v-for="d in line.forecast" :key="d.day" class="forecast-view__bar flex-column">
          <div class="forecast-view__bar-track">
            <div
              class="forecast-view__bar-fill"
              :style="{ height: (d.projected_units / maxProjected * 100) + '%' }"
              :title="`Day ${d.day}: ${d.projected_units.toFixed(1)} units`"
            />
          </div>
          <span class="forecast-view__bar-label">D{{ d.day }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.flex-row { display: flex; align-items: center; gap: 0.75rem; }
.flex-column { display: flex; flex-direction: column; gap: 0.3rem; }

.forecast-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;

  &__toolbar { justify-content: flex-start; align-items: flex-end; flex-wrap: wrap; }

  &__field {
    span { font-size: 0.75rem; color: var(--text-secondary); }
    input, select {
      padding: 0.4rem 0.6rem;
      border: 0.5px solid var(--border);
      border-radius: 6px;
      background: var(--surface-1);
      color: var(--text-primary);
      font-size: 0.8125rem;
      min-width: 220px;
    }
  }

  &__meta {
    color: var(--text-secondary);
    font-size: 0.875rem;
    &--error { color: var(--domain-danger); }
  }

  &__card {
    background: var(--surface-1);
    border: 0.5px solid var(--border);
    border-radius: 10px;
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  &__card-header { justify-content: space-between; }

  &__sku-code {
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
    font-size: 0.9375rem;
    font-weight: 600;
  }

  &__sku-name { font-size: 0.8125rem; color: var(--text-secondary); }

  &__trend {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--domain-reports, #2C8C89);
  }

  &__backend-message {
    font-size: 0.8125rem;
    color: var(--domain-danger);
    margin: 0;
  }

  &__confidence {
    font-size: 0.8125rem;
    color: var(--text-secondary);
    margin: 0;

    &--low { color: #EF9F27; } // amber, matches domain caution reuse per design doc §5.1
  }

  &__sparkline {
    align-items: flex-end;
    height: 100px;
    justify-content: flex-start;
    gap: 6px;
  }

  &__bar {
    align-items: center;
    gap: 4px;
  }

  &__bar-track {
    width: 20px;
    height: 80px;
    display: flex;
    align-items: flex-end;
  }

  &__bar-fill {
    width: 100%;
    background: var(--domain-reports, #2C8C89);
    border-radius: 3px 3px 0 0;
    min-height: 2px;
  }

  &__bar-label {
    font-size: 0.6875rem;
    color: var(--text-muted);
  }
}
</style>