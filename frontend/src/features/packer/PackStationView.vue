<!--
  PackStationView.vue
  Worker terminal for the PACKER role — same single-task skeleton as
  PickTerminalView.vue, per phase7_frontend_design.md §9.2. Task is a
  three-state machine (PENDING -> IN_PROGRESS -> COMPLETED) rather than a
  per-item loop like picking, since packing consolidates an already-picked
  list into one parcel in a single action.

  This is also the future attach point for the weigh-checkpoint reading
  (phase7_frontend_design.md §3.3) once that backend work exists — the
  "Confirm pack" action below is exactly where a measured-weight capture
  would be added.
-->
<script setup lang="ts">
import { onMounted } from 'vue';
import { usePackingTaskStore } from '@/stores/packingTaskStore';
import { useAuthStore } from '@/stores/authStore';

const store = usePackingTaskStore();
const auth = useAuthStore();

onMounted(() => store.load());
</script>

<template>
  <div class="pack-terminal">
    <header class="pack-terminal__header">
      <span class="pack-terminal__employee">{{ auth.user?.employeeId }}</span>
      <span class="pack-terminal__status">
        <span class="pack-terminal__status-dot"></span>
        PACKING
      </span>
    </header>

    <div v-if="store.loading" class="pack-terminal__meta">Loading task…</div>

    <div v-else-if="store.error && !store.task" class="pack-terminal__meta">
      {{ store.error }}
    </div>

    <template v-else-if="store.task">
      <div v-if="store.task.status !== 'COMPLETED'" class="pack-terminal__body">
        <div class="pack-terminal__card">
          <span class="pack-terminal__card-label">Pick list</span>
          <span class="pack-terminal__reference">{{ store.task.pickListId }}</span>

          <span class="pack-terminal__card-label pack-terminal__card-label--spaced">Task status</span>
          <span class="pack-terminal__task-status">{{ store.task.status }}</span>
        </div>

        <p v-if="store.error" class="pack-terminal__error">{{ store.error }}</p>

        <button
          v-if="store.task.status === 'PENDING'"
          class="pack-terminal__confirm"
          :disabled="store.submitting"
          @click="store.start"
        >
          {{ store.submitting ? 'Starting…' : 'Start packing' }}
        </button>

        <button
          v-else-if="store.task.status === 'IN_PROGRESS'"
          class="pack-terminal__confirm"
          :disabled="store.submitting"
          @click="store.complete"
        >
          {{ store.submitting ? 'Confirming…' : 'Confirm pack' }}
        </button>
      </div>

      <div v-else class="pack-terminal__complete">
        <p class="pack-terminal__complete-title">Pack complete</p>
        <template v-if="store.completedParcel">
          <p class="pack-terminal__complete-sub">Tracking</p>
          <span class="pack-terminal__reference pack-terminal__reference--large">
            {{ store.completedParcel.trackingNumber }}
          </span>
        </template>
        <p v-else class="pack-terminal__complete-sub">Parcel created — send to dispatch</p>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.pack-terminal {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 20px;
  gap: 16px;
  max-width: 400px;
  margin: 0 auto;
  width: 100%;
}

.pack-terminal__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.pack-terminal__status {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.pack-terminal__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--domain-fulfillment);
}

.pack-terminal__meta {
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  margin-top: 40px;
}

.pack-terminal__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pack-terminal__card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--surface-1);
  border-radius: 12px;
  padding: 18px;
}

.pack-terminal__card-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.pack-terminal__card-label--spaced {
  margin-top: 10px;
}

.pack-terminal__reference {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 15px;
  color: var(--text-primary);
  word-break: break-all;
}

.pack-terminal__reference--large {
  font-size: 24px;
  font-weight: 500;
}

.pack-terminal__task-status {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.pack-terminal__error {
  color: var(--domain-danger);
  font-size: 13px;
  text-align: center;
}

.pack-terminal__confirm {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--domain-fulfillment);
  color: #412402;
  border: none;
  border-radius: 12px;
  padding: 20px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }
}

.pack-terminal__complete {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 60px;
  text-align: center;
}

.pack-terminal__complete-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.pack-terminal__complete-sub {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}
</style>