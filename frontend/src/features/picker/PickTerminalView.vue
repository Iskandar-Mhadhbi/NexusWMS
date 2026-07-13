<!--
  PickTerminalView.vue
  Worker terminal for the PICKER role — single task per screen, matching
  phase7_frontend_design.md §9.2. Fetches the picker's active pick list
  (GET /pick-lists/my) and confirms a full-quantity pick per item.

  NOTE: "Skip item" is shown per the original sketch but disabled — no
  documented backend endpoint for skipping exists yet (only .../pick is
  confirmed tested, see phase4/5_progress.md). Wire it up once that
  endpoint exists rather than guessing a path now.
-->
<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { usePickListStore } from '@/stores/pickListStore';
import { useAuthStore } from '@/stores/authStore';

const store = usePickListStore();
const auth = useAuthStore();

onMounted(() => store.load());

/** 1-based index of the current item, for the "Item N of Total" label. */
const currentIndex = computed(() => store.pickedCount + 1);
</script>

<template>
  <div class="pick-terminal">
    <header class="pick-terminal__header">
      <span class="pick-terminal__employee">{{ auth.user?.employeeId }}</span>
      <span class="pick-terminal__status">
        <span class="pick-terminal__status-dot"></span>
        {{ store.isComplete ? 'COMPLETE' : 'PICKING' }}
      </span>
    </header>

    <div v-if="store.loading" class="pick-terminal__meta">Loading pick list…</div>

    <div v-else-if="store.error && !store.pickList" class="pick-terminal__meta">
      {{ store.error }}
    </div>

    <template v-else-if="store.pickList">
      <div v-if="!store.isComplete" class="pick-terminal__body">
        <div class="pick-terminal__progress-label">Item {{ currentIndex }} of {{ store.totalCount }}</div>
        <div class="pick-terminal__progress-dots">
          <span
            v-for="item in store.pickList.items"
            :key="item.id"
            class="pick-terminal__dot"
            :class="{ 'pick-terminal__dot--done': item.status !== 'PENDING' }"
          ></span>
        </div>

        <div class="pick-terminal__card">
          <span class="pick-terminal__card-label">Shelf location</span>
          <span class="pick-terminal__location">{{ store.currentItem?.shelfCode }}</span>

          <div class="pick-terminal__sku-row">
            <div class="pick-terminal__sku-col">
              <span class="pick-terminal__card-label">SKU</span>
              <span class="pick-terminal__sku">{{ store.currentItem?.skuCode ?? store.currentItem?.skuId }}</span>
            </div>
            <div class="pick-terminal__sku-col pick-terminal__sku-col--right">
              <span class="pick-terminal__card-label">Quantity</span>
              <span class="pick-terminal__sku">&times;{{ store.currentItem?.quantityToPick }}</span>
            </div>
          </div>
        </div>

        <p v-if="store.error" class="pick-terminal__error">{{ store.error }}</p>

        <button class="pick-terminal__confirm" :disabled="store.submitting" @click="store.confirmCurrentItem">
          {{ store.submitting ? 'Confirming…' : 'Scan to confirm' }}
        </button>

        <button class="pick-terminal__skip" disabled title="No backend skip endpoint confirmed yet">
          Skip item
        </button>
      </div>

      <div v-else class="pick-terminal__complete">
        <p class="pick-terminal__complete-title">Pick list complete</p>
        <p class="pick-terminal__complete-sub">Return cart to packing</p>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.pick-terminal {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 20px;
  gap: 16px;
  max-width: 400px;
  margin: 0 auto;
  width: 100%;
}

.pick-terminal__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.pick-terminal__status {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.pick-terminal__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--domain-fulfillment);
}

.pick-terminal__meta {
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  margin-top: 40px;
}

.pick-terminal__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pick-terminal__progress-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.pick-terminal__progress-dots {
  display: flex;
  flex-direction: row;
  gap: 6px;
}

.pick-terminal__dot {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: var(--border);
}

.pick-terminal__dot--done {
  background: var(--domain-fulfillment);
}

.pick-terminal__card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--surface-1);
  border-radius: 12px;
  padding: 18px;
}

.pick-terminal__card-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.pick-terminal__location {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 32px;
  font-weight: 500;
  color: var(--text-primary);
}

.pick-terminal__sku-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  margin-top: 8px;
}

.pick-terminal__sku-col {
  display: flex;
  flex-direction: column;
}

.pick-terminal__sku-col--right {
  align-items: flex-end;
}

.pick-terminal__sku {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 15px;
  color: var(--text-primary);
}

.pick-terminal__error {
  color: var(--domain-danger);
  font-size: 13px;
  text-align: center;
}

.pick-terminal__confirm {
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

.pick-terminal__skip {
  background: transparent;
  color: var(--text-muted);
  border: 0.5px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  font-size: 13px;
  cursor: not-allowed;
}

.pick-terminal__complete {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 60px;
  text-align: center;
}

.pick-terminal__complete-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.pick-terminal__complete-sub {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}
</style>