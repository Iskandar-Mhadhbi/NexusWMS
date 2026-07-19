<!--
  Invoice queue — Finance pillar.
  List + create + trigger 3-way match. Status and match-status shown as
  plain text (no badge component available in this project — see prior
  session note).
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useInvoiceStore } from '@/stores/invoiceStore';
import { CreateInvoiceSchema, type CreateInvoiceRequest } from '@/core/models/invoice';

const store = useInvoiceStore();
const showCreateForm = ref(false);
const formError = ref<string | null>(null);

const form = ref<CreateInvoiceRequest>({
  purchaseOrderId: '',
  invoiceNumber: '',
  invoiceAmount: 0,
});

/** Validate via Zod, then submit. Mirrors the pattern used on Zones/Stock/Orders. */
async function submitCreate() {
  formError.value = null;
  const result = CreateInvoiceSchema.safeParse(form.value);
  if (!result.success) {
    formError.value = result.error.issues[0].message;
    return;
  }
  await store.create(result.data);
  showCreateForm.value = false;
  form.value = { purchaseOrderId: '', invoiceNumber: '', invoiceAmount: 0 };
}

/** Trigger 3-way match for a given invoice row. */
async function runMatch(id: string) {
  await store.match(id);
}

onMounted(() => store.fetchAll());
</script>

<template>
  <div class="invoices-view">
    <div class="invoices-view__toolbar flex-row">
      <button class="invoices-view__new-btn" @click="showCreateForm = !showCreateForm">
        New invoice
      </button>
    </div>

    <div v-if="showCreateForm" class="invoices-view__form flex-column">
      <input v-model="form.purchaseOrderId" placeholder="Purchase order UUID" />
      <input v-model="form.invoiceNumber" placeholder="Invoice number" />
      <input v-model.number="form.invoiceAmount" type="number" step="0.01" placeholder="Amount" />
      <p v-if="formError" class="invoices-view__error">{{ formError }}</p>
      <button @click="submitCreate">Submit</button>
    </div>

    <p v-if="store.loading" class="invoices-view__meta">Loading…</p>
    <p v-else-if="store.error" class="invoices-view__meta invoices-view__meta--error">{{ store.error }}</p>
    <p v-else-if="store.invoices.length === 0" class="invoices-view__meta">No invoices found.</p>

    <table v-else class="invoices-view__table">
      <thead>
        <tr>
          <th>Invoice #</th>
          <th>PO #</th>
          <th>Supplier</th>
          <th>Amount</th>
          <th>Status</th>
          <th>Match status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="inv in store.invoices" :key="inv.id">
          <td class="invoices-view__mono">{{ inv.invoiceNumber }}</td>
          <td class="invoices-view__mono">{{ inv.poNumber }}</td>
          <td>{{ inv.supplierName }}</td>
          <td>{{ inv.invoiceAmount.toFixed(2) }}</td>
          <td>{{ inv.status }}</td>
          <td>{{ inv.threeWayMatchStatus }}</td>
          <td>
            <button
              v-if="inv.threeWayMatchStatus === 'PENDING'"
              @click="runMatch(inv.id)"
            >
              Run match
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style lang="scss" scoped>
.flex-row { display: flex; align-items: center; gap: 0.75rem; }
.flex-column { display: flex; flex-direction: column; gap: 0.5rem; }

.invoices-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  &__toolbar {
    justify-content: flex-start;
  }

  &__new-btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 6px;
    background: var(--domain-finance, #C85A8C);
    color: white;
    cursor: pointer;
    font-size: 0.875rem;
    font-weight: 600;
  }

  &__form {
    padding: 1rem;
    border: 0.5px solid var(--border);
    border-radius: 8px;
    background: var(--surface-1);
    max-width: 400px;

    input {
      padding: 0.5rem;
      border: 0.5px solid var(--border);
      border-radius: 6px;
      background: var(--surface-0);
      color: var(--text-primary);
    }

    button {
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 6px;
      background: var(--domain-finance, #C85A8C);
      color: white;
      cursor: pointer;
      align-self: flex-start;
    }
  }

  &__error {
    color: var(--domain-danger);
    font-size: 0.8125rem;
  }

  &__meta {
    color: var(--text-secondary);
    font-size: 0.875rem;

    &--error {
      color: var(--domain-danger);
    }
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    background: var(--surface-1);
    border: 0.5px solid var(--border);
    border-radius: 8px;
    overflow: hidden;

    th, td {
      text-align: left;
      padding: 0.6rem 1rem;
      border-bottom: 0.5px solid var(--border);
      font-size: 0.875rem;
    }

    th {
      color: var(--text-secondary);
      font-weight: 600;
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    tr:last-child td {
      border-bottom: none;
    }

    button {
      padding: 0.3rem 0.7rem;
      border: 0.5px solid var(--domain-finance, #C85A8C);
      border-radius: 6px;
      background: transparent;
      color: var(--domain-finance, #C85A8C);
      cursor: pointer;
      font-size: 0.8125rem;
    }
  }

  &__mono {
    font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
    font-size: 0.8125rem;
    color: var(--text-secondary);
  }
}
</style>