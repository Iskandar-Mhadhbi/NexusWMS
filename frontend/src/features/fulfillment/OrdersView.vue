<!--
  OrdersView.vue
  Fulfillment → Orders tab. List + create (with line items) + cancel +
  generate-fulfillment-request. Entry point to the whole Fulfillment
  pipeline — a manager starts the chain here that pick-lists/packing/
  dispatch (later sub-tabs) continue.

  customerAddress is NOT collected in this form — CreateOrderSchema
  defaults it to {} (core/models/order.ts), same deliberate deferral
  SkusView.vue already established for Sku.dimensions: a flexible JSONB
  field with no confirmed sub-shape, not worth guessing at yet.

  SKU line items use pasted UUID for now — same known gap Procurement's
  PO form had before skuStore existed. skuStore now exists (Inventory
  pillar) so this SHOULD be retrofitted later; flagged, not blocking.
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useOrderStore } from '@/stores/orderStore';
import { CreateOrderSchema, type OrderPriority } from '@/core/models/order';

const store = useOrderStore();

const showForm = ref(false);
const submitError = ref('');

const priorities: OrderPriority[] = ['STANDARD', 'EXPRESS', 'URGENT'];

const form = ref<{
  customerName: string;
  priority: OrderPriority;
  notes: string;
  lines: { skuId: string; quantityOrdered: number | null; unitPrice: number | null }[];
}>({
  customerName: '',
  priority: 'STANDARD',
  notes: '',
  lines: [{ skuId: '', quantityOrdered: null, unitPrice: null }],
});

onMounted(() => store.fetchAll());

function addLine() {
  form.value.lines.push({ skuId: '', quantityOrdered: null, unitPrice: null });
}

function removeLine(index: number) {
  form.value.lines.splice(index, 1);
}

/** Validates the form against CreateOrderSchema (customerAddress auto-filled to {} by the schema's .default()) before calling the store. */
async function handleCreate() {
  submitError.value = '';
  const result = CreateOrderSchema.safeParse({
    customerName: form.value.customerName,
    priority: form.value.priority,
    notes: form.value.notes.trim() || null,
    lines: form.value.lines,
  });
  if (!result.success) {
    submitError.value = result.error.issues[0].message;
    return;
  }
  try {
    await store.create(result.data);
    form.value = { customerName: '', priority: 'STANDARD', notes: '', lines: [{ skuId: '', quantityOrdered: null, unitPrice: null }] };
    showForm.value = false;
  } catch (err) {
    submitError.value = 'Could not create order — check the fields and try again.';
    console.error('[OrdersView] create failed:', err);
  }
}

/** Backend rejects cancellation once an order is PICKING or beyond — that rejection is expected, not a bug. */
async function handleCancel(id: string) {
  submitError.value = '';
  try {
    await store.cancel(id);
  } catch (err) {
    submitError.value = 'Could not cancel this order — it may have already moved past VALIDATED.';
    console.error('[OrdersView] cancel failed:', err);
  }
}

async function handleGenerateFulfillment(id: string) {
  submitError.value = '';
  try {
    await store.generateFulfillmentRequest(id);
  } catch (err) {
    submitError.value = 'Could not generate a fulfillment request for this order.';
    console.error('[OrdersView] generateFulfillmentRequest failed:', err);
  }
}
</script>

<template>
  <div class="orders">
    <div class="orders__header">
      <h1 class="orders__title">Orders</h1>
      <button class="orders__new-btn" @click="showForm = !showForm">
        {{ showForm ? 'Cancel' : 'New order' }}
      </button>
    </div>

    <form v-if="showForm" class="orders__form" @submit.prevent="handleCreate">
      <input v-model="form.customerName" placeholder="Customer name" />
      <select v-model="form.priority">
        <option v-for="p in priorities" :key="p" :value="p">{{ p }}</option>
      </select>
      <input v-model="form.notes" placeholder="Notes (optional)" />

      <div class="orders__lines">
        <div v-for="(line, i) in form.lines" :key="i" class="orders__line">
          <input v-model="line.skuId" placeholder="SKU UUID (temporary — pasted manually until retrofitted onto the SKU catalog)" class="orders__line-sku" />
          <input v-model.number="line.quantityOrdered" type="number" min="1" placeholder="Qty" class="orders__line-qty" />
          <input v-model.number="line.unitPrice" type="number" min="0" step="0.01" placeholder="Unit price" class="orders__line-price" />
          <button type="button" class="orders__line-remove" @click="removeLine(i)" :disabled="form.lines.length === 1">×</button>
        </div>
        <button type="button" class="orders__add-line-btn" @click="addLine">+ Add line</button>
      </div>

      <button type="submit" :disabled="store.creating">{{ store.creating ? 'Saving…' : 'Save order' }}</button>
    </form>

    <p v-if="submitError" class="orders__error">{{ submitError }}</p>
    <p v-if="store.error" class="orders__error">{{ store.error }}</p>
    <p v-if="store.loading" class="orders__meta">Loading orders…</p>

    <div v-else class="orders__list">
      <div class="orders-head">
        <span class="orders-head__col orders-head__col--number">Order #</span>
        <span class="orders-head__col orders-head__col--customer">Customer</span>
        <span class="orders-head__col orders-head__col--priority">Priority</span>
        <span class="orders-head__col orders-head__col--status">Status</span>
        <span class="orders-head__col orders-head__col--actions">Actions</span>
      </div>

      <div v-for="order in store.orders" :key="order.id" class="order-row">
        <span class="order-row__number">{{ order.orderNumber }}</span>
        <span class="order-row__customer">{{ order.customerName }}</span>
        <span class="order-row__priority">{{ order.priority }}</span>
        <span class="order-row__status" :class="`order-row__status--${order.status.toLowerCase()}`">{{ order.status }}</span>
        <div class="order-row__actions">
          <button
            v-if="order.status === 'RECEIVED'"
            class="order-row__action-btn"
            @click="handleGenerateFulfillment(order.id)"
          >
            Generate fulfillment
          </button>
          <button
            v-if="!['DISPATCHED', 'CANCELLED'].includes(order.status)"
            class="order-row__action-btn order-row__action-btn--danger"
            @click="handleCancel(order.id)"
          >
            Cancel
          </button>
        </div>
      </div>
      <p v-if="store.orders.length === 0" class="orders__meta">No orders found.</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.orders {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  flex: 1;
}

.orders__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.orders__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.orders__new-btn {
  background: var(--domain-fulfillment);
  color: var(--surface-1);
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
}

.orders__form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 10px;
  padding: 1rem;
  max-width: 480px;

  input,
  select {
    padding: 8px 10px;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    font-size: 14px;
    background: var(--surface-0);
    color: var(--text-primary);
  }

  > button[type='submit'] {
    background: var(--domain-fulfillment);
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

.orders__lines {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: var(--surface-0);
  border-radius: 6px;
}

.orders__line {
  display: flex;
  flex-direction: row;
  gap: 6px;
}

.orders__line-sku {
  flex: 2;
}
.orders__line-qty {
  flex: 1;
}
.orders__line-price {
  flex: 1;
}

.orders__line-remove {
  background: none;
  border: 0.5px solid var(--border);
  color: var(--domain-danger);
  border-radius: 6px;
  width: 32px;
  cursor: pointer;

  &:disabled {
    opacity: 0.4;
    cursor: default;
  }
}

.orders__add-line-btn {
  align-self: flex-start;
  background: none;
  border: 0.5px dashed var(--domain-fulfillment);
  color: var(--domain-fulfillment);
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.orders__meta {
  color: var(--text-muted);
  font-size: 14px;
}

.orders__error {
  color: var(--domain-danger);
  font-size: 13px;
}

.orders__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.orders-head {
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

.orders-head__col {
  &--number { flex: 0 0 160px; }
  &--customer { flex: 1; }
  &--priority { flex: 0 0 80px; }
  &--status { flex: 0 0 100px; }
  &--actions { flex: 0 0 220px; }
}

.order-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;
}

.order-row__number {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  color: var(--text-secondary);
  flex: 0 0 160px;
}

.order-row__customer {
  font-size: 14px;
  color: var(--text-primary);
  flex: 1;
}

.order-row__priority {
  font-size: 12px;
  color: var(--text-secondary);
  flex: 0 0 80px;
}

.order-row__status {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  flex: 0 0 100px;
  text-align: center;

  // RECEIVED/VALIDATED deliberately neutral, not borrowed from another
  // domain's accent — domain colors mean "which pillar owns this," not
  // "how far along is this order." PICKING/PACKING/DISPATCHED/CANCELLED
  // use their real semantic colors per phase7_frontend_design.md.
  &--received { color: var(--text-secondary); background: var(--surface-0); }
  &--validated { color: var(--text-secondary); background: var(--surface-0); }
  &--picking, &--packing { color: var(--domain-fulfillment); background: color-mix(in srgb, var(--domain-fulfillment) 15%, transparent); }
  &--dispatched { color: var(--domain-dispatch); background: color-mix(in srgb, var(--domain-dispatch) 12%, transparent); }
  &--cancelled { color: var(--domain-danger); background: color-mix(in srgb, var(--domain-danger) 12%, transparent); }
}

.order-row__actions {
  display: flex;
  flex-direction: row;
  gap: 6px;
  flex: 0 0 220px;
}

.order-row__action-btn {
  background: none;
  border: 0.5px solid var(--border);
  color: var(--text-secondary);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;

  &--danger {
    border-color: var(--domain-danger);
    color: var(--domain-danger);
  }
}
</style>