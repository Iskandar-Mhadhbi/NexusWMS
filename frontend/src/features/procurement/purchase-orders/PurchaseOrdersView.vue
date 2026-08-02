<!--
  PurchaseOrdersView.vue
  Procurement pillar — purchase order list + create form. Line items
  reference SKUs by pasted UUID for now, since the Inventory pillar (where
  a real SKU picker would live) isn't built yet — see
  phase7_frontend_design.md §10. Revisit once Inventory/sku-management
  exists.

  requestedBy/approvedBy are enriched UserSummary objects (actor
  enrichment pass, see actor_field_enrichment_progress.md) — displayed by
  employeeId, the human-readable identifier convention used everywhere
  else in this app. supplierName now comes directly from the backend DTO,
  so the old local lookup-by-id helper was removed.
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { usePurchaseOrderStore } from '@/stores/purchaseOrderStore';
import { useSupplierStore } from '@/stores/supplierStore';
import type { CreatePurchaseOrderPayload } from '@/core/models/purchaseOrder';

const store = usePurchaseOrderStore();
const supplierStore = useSupplierStore();

const showForm = ref(false);
const submitting = ref(false);
const submitError = ref('');
const approvingId = ref<string | null>(null);

const emptyLine = () => ({ skuId: '', quantityOrdered: 1, unitPrice: 0 });

const form = ref<CreatePurchaseOrderPayload>({
  supplierId: '',
  expectedDelivery: null,
  lines: [emptyLine()],
});

onMounted(() => {
  store.load();
  supplierStore.load();
});

/** Adds a blank line item row to the form. */
function addLine() {
  form.value.lines.push(emptyLine());
}

/** Removes a line item row by index; always leaves at least one row. */
function removeLine(index: number) {
  if (form.value.lines.length > 1) {
    form.value.lines.splice(index, 1);
  }
}

/** Submits the new-PO form; resets and closes it on success. */
async function handleCreate() {
  submitting.value = true;
  submitError.value = '';
  try {
    await store.create(form.value);
    form.value = { supplierId: '', expectedDelivery: null, lines: [emptyLine()] };
    showForm.value = false;
  } catch (err) {
    submitError.value = 'Could not create purchase order — check the fields and try again.';
    console.error('[PurchaseOrdersView] create failed:', err);
  } finally {
    submitting.value = false;
  }
}

/** Approves a purchase order. ADMIN/MANAGER only, per backend @PreAuthorize. */
async function handleApprove(id: string) {
  approvingId.value = id;
  try {
    await store.approve(id);
  } catch (err) {
    console.error('[PurchaseOrdersView] approve failed:', err);
  } finally {
    approvingId.value = null;
  }
}
</script>

<template>
  <div class="pos">
    <div class="pos__header">
      <h1 class="pos__title">Purchase orders</h1>
      <button class="pos__new-btn" @click="showForm = !showForm">
        {{ showForm ? 'Cancel' : 'New purchase order' }}
      </button>
    </div>

    <form v-if="showForm" class="pos__form" @submit.prevent="handleCreate">
      <select v-model="form.supplierId" required>
        <option value="" disabled>Select supplier</option>
        <option v-for="s in supplierStore.suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>

      <input v-model="form.expectedDelivery" type="date" placeholder="Expected delivery" />

      <div class="pos__lines">
        <div v-for="(line, i) in form.lines" :key="i" class="pos__line">
          <input v-model="line.skuId" placeholder="SKU UUID" required />
          <input v-model.number="line.quantityOrdered" type="number" min="1" placeholder="Qty" required />
          <input v-model.number="line.unitPrice" type="number" min="0" step="0.01" placeholder="Unit price" required />
          <button type="button" class="pos__remove-line" @click="removeLine(i)">&times;</button>
        </div>
        <button type="button" class="pos__add-line" @click="addLine">+ Add line</button>
      </div>

      <button type="submit" :disabled="submitting">{{ submitting ? 'Saving…' : 'Save' }}</button>
      <p v-if="submitError" class="pos__error">{{ submitError }}</p>
    </form>

    <p v-if="store.loading" class="pos__meta">Loading…</p>
    <p v-else-if="store.error" class="pos__error">{{ store.error }}</p>

    <div v-else class="pos__list">
      <div v-for="po in store.purchaseOrders" :key="po.id" class="po-row">
        <span class="po-row__number">{{ po.poNumber }}</span>
        <span class="po-row__supplier">{{ po.supplierName }}</span>
        <span class="po-row__actor">
          Requested by {{ po.requestedBy?.employeeId ?? '—' }}
          <template v-if="po.approvedBy"> · Approved by {{ po.approvedBy.employeeId }}</template>
        </span>
        <span class="po-row__total">${{ po.totalAmount.toFixed(2) }}</span>
        <span class="po-row__status" :class="`po-row__status--${po.status.toLowerCase()}`">
          {{ po.status }}
        </span>
        <button
          v-if="po.status === 'DRAFT' || po.status === 'PENDING_APPROVAL'"
          class="po-row__approve"
          :disabled="approvingId === po.id"
          @click="handleApprove(po.id)"
        >
          {{ approvingId === po.id ? 'Approving…' : 'Approve' }}
        </button>
      </div>
      <p v-if="store.purchaseOrders.length === 0" class="pos__meta">No purchase orders yet.</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.pos {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  flex: 1;
}

.pos__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.pos__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.pos__new-btn {
  background: var(--domain-procurement);
  color: var(--surface-1);
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
}

.pos__form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 10px;
  padding: 1rem;
  max-width: 480px;

  select,
  input {
    padding: 8px 10px;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    font-size: 14px;
    background: var(--surface-0);
    color: var(--text-primary);
  }

  button[type='submit'] {
    background: var(--domain-procurement);
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

.pos__lines {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pos__line {
  display: flex;
  flex-direction: row;
  gap: 6px;

  input {
    flex: 1;
  }
}

.pos__remove-line {
  background: none;
  border: 0.5px solid var(--border);
  border-radius: 6px;
  color: var(--text-secondary);
  width: 32px;
  flex-shrink: 0;
  cursor: pointer;

  &:hover {
    color: var(--domain-danger);
    border-color: var(--domain-danger);
  }
}

.pos__add-line {
  align-self: flex-start;
  background: none;
  border: none;
  color: var(--domain-procurement);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
}

.pos__meta {
  color: var(--text-muted);
  font-size: 14px;
}

.pos__error {
  color: var(--domain-danger);
  font-size: 13px;
}

.pos__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.po-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;
}

.po-row__number {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  color: var(--text-secondary);
  width: 140px;
  flex-shrink: 0;
}

.po-row__supplier {
  font-size: 14px;
  color: var(--text-primary);
  flex: 1;
}

.po-row__actor {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}

.po-row__total {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  color: var(--text-secondary);
}

.po-row__status {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
  white-space: nowrap;
}

.po-row__status--draft {
  color: var(--text-muted);
  background: color-mix(in srgb, var(--text-muted) 12%, transparent);
}

.po-row__status--pending_approval {
  color: var(--domain-fulfillment);
  background: color-mix(in srgb, var(--domain-fulfillment) 12%, transparent);
}

.po-row__status--approved,
.po-row__status--fully_received {
  color: var(--domain-dispatch);
  background: color-mix(in srgb, var(--domain-dispatch) 12%, transparent);
}

.po-row__status--partially_received {
  color: var(--domain-procurement);
  background: color-mix(in srgb, var(--domain-procurement) 12%, transparent);
}

.po-row__status--cancelled {
  color: var(--domain-danger);
  background: color-mix(in srgb, var(--domain-danger) 12%, transparent);
}

.po-row__approve {
  background: var(--domain-dispatch);
  color: var(--surface-1);
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }
}
</style>