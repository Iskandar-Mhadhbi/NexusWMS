<!--
  InboundReceivingView.vue
  Worker terminal for the RECEIVER role. Structurally different from
  Pick/Pack/Dispatch: tied to a purchase order rather than a per-worker
  queue or single task (per phase7_frontend_design.md §9.2 — "Receiver —
  tied to a PO/goods receipt instead of a pick list"). Two-step flow:
  select an APPROVED PO, then enter received quantity/batch/shelf per line
  and submit as one GoodsReceipt.

  Reuses purchaseOrderStore (built for the Procurement pillar) for the PO
  list rather than a separate fetch — same data, second consumer.
-->
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { usePurchaseOrderStore } from '@/stores/purchaseOrderStore';
import { useGoodsReceiptStore } from '@/stores/goodsReceiptStore';
import { useAuthStore } from '@/stores/authStore';
import type { GoodsReceiptLinePayload } from '@/core/models/goodsReceipt';

const poStore = usePurchaseOrderStore();
const grStore = useGoodsReceiptStore();
const auth = useAuthStore();

const selectedPoId = ref<string | null>(null);
const notes = ref('');

/** Per-line form input, keyed by the PO line's own id. */
const lineInputs = reactive<Record<string, { quantityReceived: number; batchId: string; expiryDate: string; shelfId: string }>>({});

onMounted(() => poStore.load());

const approvedPos = computed(() => poStore.purchaseOrders.filter((po) => po.status === 'APPROVED'));

const selectedPo = computed(() => poStore.purchaseOrders.find((po) => po.id === selectedPoId.value) ?? null);

/** Selects a PO and seeds one blank input row per PO line. */
function selectPo(poId: string) {
  selectedPoId.value = poId;
  const po = poStore.purchaseOrders.find((p) => p.id === poId);
  if (!po) return;
  for (const line of po.lines) {
    lineInputs[line.id] = {
      quantityReceived: line.quantityOrdered - line.quantityReceived,
      batchId: '',
      expiryDate: '',
      shelfId: '',
    };
  }
}

/** Submits the full receipt: header + one line per PO line. */
async function handleSubmit() {
  if (!selectedPo.value) return;

  const lines: GoodsReceiptLinePayload[] = selectedPo.value.lines.map((line) => ({
    poLineId: line.id,
    skuId: line.skuId,
    quantityReceived: lineInputs[line.id]?.quantityReceived ?? line.quantityOrdered,
    batchId: lineInputs[line.id]?.batchId ?? '',
    expiryDate: lineInputs[line.id]?.expiryDate || null,
    shelfId: lineInputs[line.id]?.shelfId ?? '',
  }));

  try {
    await grStore.submit({ purchaseOrderId: selectedPo.value.id, notes: notes.value, lines });
    selectedPoId.value = null;
    notes.value = '';
  } catch {
    // error already captured in grStore.error, surfaced in the template
  }
}

/** Returns to the PO-picker step to log another receipt. */
function startNew() {
  grStore.reset();
  selectedPoId.value = null;
}
</script>

<template>
  <div class="receiving-terminal">
    <header class="receiving-terminal__header">
      <span class="receiving-terminal__employee">{{ auth.user?.employeeId }}</span>
      <span class="receiving-terminal__status">
        <span class="receiving-terminal__status-dot"></span>
        RECEIVING
      </span>
    </header>

    <!-- Step: success -->
    <div v-if="grStore.lastReceipt" class="receiving-terminal__complete">
      <p class="receiving-terminal__complete-title">Receipt logged</p>
      <span class="receiving-terminal__reference">{{ grStore.lastReceipt.grNumber }}</span>
      <button class="receiving-terminal__confirm" @click="startNew">Log another receipt</button>
    </div>

    <!-- Step: PO picker -->
    <div v-else-if="!selectedPo" class="receiving-terminal__body">
      <p v-if="poStore.loading" class="receiving-terminal__meta">Loading purchase orders…</p>
      <template v-else>
        <p class="receiving-terminal__meta">Select a purchase order to receive against</p>
        <button
          v-for="po in approvedPos"
          :key="po.id"
          class="po-pick"
          @click="selectPo(po.id)"
        >
          <span class="po-pick__number">{{ po.poNumber }}</span>
          <span class="po-pick__lines">{{ po.lines.length }} lines</span>
        </button>
        <p v-if="approvedPos.length === 0" class="receiving-terminal__meta">
          No approved purchase orders awaiting receipt.
        </p>
      </template>
    </div>

    <!-- Step: line entry -->
    <div v-else class="receiving-terminal__body">
      <span class="receiving-terminal__reference">{{ selectedPo.poNumber }}</span>

      <div v-for="line in selectedPo.lines" :key="line.id" class="receipt-line">
        <span class="receipt-line__sku">{{ line.skuCode ?? line.skuId }}</span>
        <span class="receipt-line__ordered">Ordered &times;{{ line.quantityOrdered }}</span>

        <div class="receipt-line__inputs">
          <input v-model.number="lineInputs[line.id].quantityReceived" type="number" min="0" placeholder="Qty received" />
          <input v-model="lineInputs[line.id].batchId" placeholder="Batch ID" />
          <input v-model="lineInputs[line.id].expiryDate" type="date" />
          <input v-model="lineInputs[line.id].shelfId" placeholder="Shelf UUID" />
        </div>
      </div>

      <input v-model="notes" placeholder="Notes (optional)" class="receiving-terminal__notes" />

      <p v-if="grStore.error" class="receiving-terminal__error">{{ grStore.error }}</p>

      <button class="receiving-terminal__confirm" :disabled="grStore.submitting" @click="handleSubmit">
        {{ grStore.submitting ? 'Submitting…' : 'Confirm receipt' }}
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.receiving-terminal {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 20px;
  gap: 16px;
  max-width: 420px;
  margin: 0 auto;
  width: 100%;
}

.receiving-terminal__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.receiving-terminal__status {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 6px;
}

.receiving-terminal__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--domain-procurement);
}

.receiving-terminal__meta {
  color: var(--text-secondary);
  font-size: 14px;
}

.receiving-terminal__error {
  color: var(--domain-danger);
  font-size: 13px;
}

.receiving-terminal__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.receiving-terminal__reference {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

.po-pick {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  background: var(--surface-1);
  border: none;
  border-radius: 10px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.po-pick__number {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 14px;
  color: var(--text-primary);
}

.po-pick__lines {
  font-size: 12px;
  color: var(--text-secondary);
}

.receipt-line {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--surface-1);
  border-radius: 10px;
  padding: 14px;
}

.receipt-line__sku {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 14px;
  color: var(--text-primary);
}

.receipt-line__ordered {
  font-size: 12px;
  color: var(--text-secondary);
}

.receipt-line__inputs {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;

  input {
    flex: 1;
    min-width: 90px;
    padding: 8px;
    border-radius: 6px;
    border: 0.5px solid var(--border);
    background: var(--surface-0);
    color: var(--text-primary);
    font-size: 13px;
  }
}

.receiving-terminal__notes {
  padding: 10px;
  border-radius: 8px;
  border: 0.5px solid var(--border);
  background: var(--surface-1);
  color: var(--text-primary);
  font-size: 14px;
}

.receiving-terminal__confirm {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  background: var(--domain-procurement);
  color: var(--surface-1);
  border: none;
  border-radius: 12px;
  padding: 16px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }
}

.receiving-terminal__complete {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 60px;
  text-align: center;
}

.receiving-terminal__complete-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}
</style>