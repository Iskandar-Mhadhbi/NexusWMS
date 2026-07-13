<!--
  FlowRail.vue
  Live horizontal cross-section of the five warehouse zones — the manager
  dashboard's signature element (phase7_frontend_design.md §5.4). Renders
  state owned by stores/liveOpsStore.ts, fed by the dashboard WebSocket
  connection opened once at the ManagerShell level.
-->
<script setup lang="ts">
import { useLiveOpsStore } from '@/stores/liveOpsStore';

const store = useLiveOpsStore();

const BOTTLENECK_THRESHOLD = 80;

/** @returns true once a zone's occupancy crosses the bottleneck threshold */
function isBottleneck(occupancyPercent: number): boolean {
  return occupancyPercent >= BOTTLENECK_THRESHOLD;
}
</script>

<template>
  <div class="flow-rail">
    <div class="flow-rail__header">
      <span class="flow-rail__title">NexusWMS · Manager</span>
      <span class="flow-rail__live">
        <span
          class="flow-rail__live-dot"
          :class="{ 'flow-rail__live-dot--off': !store.connected }"
        ></span>
        {{ store.connected ? 'Live' : 'Connecting…' }}
      </span>
    </div>

    <div class="flow-rail__zones">
      <div
        v-for="zone in store.zones"
        :key="zone.key"
        class="zone"
        :class="[`zone--${zone.domain}`, { 'zone--bottleneck': isBottleneck(zone.occupancyPercent) }]"
      >
        <div class="zone__label">{{ zone.label }}</div>

        <div class="zone__body">
          <div v-for="dot in zone.dots" :key="dot.id" class="zone__dot"></div>

          <div v-if="store.workerCount(zone.key) > 0" class="zone__worker-chip">
            {{ store.workerCount(zone.key) }} <span class="zone__worker-label">on floor</span>
          </div>
        </div>

        <div class="zone__fill-track">
          <div class="zone__fill" :style="{ width: `${zone.occupancyPercent}%` }"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.flow-rail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--surface-1);
  border-radius: 12px;
  padding: 1rem;
}

.flow-rail__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  font-family: "JetBrains Mono", "SF Mono", Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.flow-rail__live {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.flow-rail__live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--domain-dispatch);
}

.flow-rail__live-dot--off {
  background: var(--text-muted);
}

.flow-rail__zones {
  display: flex;
  flex-direction: row;
  border-radius: 10px;
  overflow: hidden;
}

.zone {
  display: flex;
  flex-direction: column;
  flex: 1;
  position: relative;
  min-height: 90px;
  padding: 10px 12px;
  gap: 6px;
}

.zone--procurement { background: color-mix(in srgb, var(--domain-procurement) 12%, var(--surface-1)); }
.zone--inventory { background: color-mix(in srgb, var(--domain-inventory) 10%, var(--surface-1)); }
.zone--fulfillment { background: color-mix(in srgb, var(--domain-fulfillment) 14%, var(--surface-1)); }
.zone--dispatch { background: color-mix(in srgb, var(--domain-dispatch) 12%, var(--surface-1)); }

.zone--procurement .zone__label { color: var(--domain-procurement); }
.zone--inventory .zone__label { color: var(--domain-inventory); }
.zone--fulfillment .zone__label { color: var(--domain-fulfillment); }
.zone--dispatch .zone__label { color: var(--domain-dispatch); }

.zone__label {
  font-size: 12px;
  font-weight: 600;
}

.zone__body {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  flex: 1;
  position: relative;
}

// Dot lifetime (3.5s) must match DOT_LIFETIME_MS in liveOpsStore.ts —
// the JS removal and the CSS fade-out are meant to land together.
.zone__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--text-primary);
  animation: drift 3.5s linear;
}

@keyframes drift {
  0% { transform: translateX(0); opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateX(40px); opacity: 0; }
}

.zone__worker-chip {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  gap: 4px;
  margin-left: auto;
  font-family: "JetBrains Mono", "SF Mono", Consolas, monospace;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary);
}

.zone__worker-label {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-weight: 400;
  color: var(--text-secondary);
}

.zone__fill-track {
  height: 4px;
  border-radius: 2px;
  background: color-mix(in srgb, var(--text-primary) 10%, transparent);
  overflow: hidden;
}

.zone__fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.zone--procurement .zone__fill { background: var(--domain-procurement); }
.zone--inventory .zone__fill { background: var(--domain-inventory); }
.zone--fulfillment .zone__fill { background: var(--domain-fulfillment); }
.zone--dispatch .zone__fill { background: var(--domain-dispatch); }

.zone--bottleneck {
  animation: pulse-danger 1.6s ease-in-out infinite;
}

.zone--bottleneck .zone__fill {
  background: var(--domain-danger);
}

.zone--bottleneck .zone__label::after {
  content: " ⚠";
}

@keyframes pulse-danger {
  0%, 100% { box-shadow: inset 0 0 0 0 color-mix(in srgb, var(--domain-danger) 40%, transparent); }
  50% { box-shadow: inset 0 0 0 3px color-mix(in srgb, var(--domain-danger) 40%, transparent); }
}
</style>