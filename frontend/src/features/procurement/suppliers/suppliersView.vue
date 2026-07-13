<!--
  SuppliersView.vue
  Procurement pillar — supplier list + create form. This is the reference
  implementation: list + create-form + Pinia-store pattern that Purchase
  Orders, Goods Receipts, and the other five pillar pages should copy.
  See phase7_frontend_design.md §6, §10.

  NOTE: Purchase Orders and Goods Receipts aren't built yet — once they
  exist, this page grows tab-style sub-navigation rather than staying a
  single flat view.
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useSupplierStore } from '@/stores/supplierStore';
import type { CreateSupplierPayload } from '@/core/models/supplier';

const store = useSupplierStore();
const showForm = ref(false);
const submitting = ref(false);
const submitError = ref('');

const form = ref<CreateSupplierPayload>({
  name: '',
  code: '',
  contactInfo: {},
  paymentTerms: '',
  rating: 0,
});

onMounted(() => store.load());

/** Submits the new-supplier form; resets and closes it on success. */
async function handleCreate() {
  submitting.value = true;
  submitError.value = '';
  try {
    await store.create(form.value);
    form.value = { name: '', code: '', contactInfo: {}, paymentTerms: '', rating: 0 };
    showForm.value = false;
  } catch (err) {
    submitError.value = 'Could not create supplier — check the fields and try again.';
    console.error('[SuppliersView] create failed:', err);
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="suppliers">
    <div class="suppliers__header">
      <h1 class="suppliers__title">Suppliers</h1>
      <button class="suppliers__new-btn" @click="showForm = !showForm">
        {{ showForm ? 'Cancel' : 'New supplier' }}
      </button>
    </div>

    <form v-if="showForm" class="suppliers__form" @submit.prevent="handleCreate">
      <input v-model="form.name" placeholder="Name" required />
      <input v-model="form.code" placeholder="Code (e.g. SUP-001)" required />
      <input v-model="form.paymentTerms" placeholder="Payment terms" />
      <input v-model.number="form.rating" type="number" min="0" max="5" placeholder="Rating" />
      <button type="submit" :disabled="submitting">{{ submitting ? 'Saving…' : 'Save' }}</button>
      <p v-if="submitError" class="suppliers__error">{{ submitError }}</p>
    </form>

    <p v-if="store.loading" class="suppliers__meta">Loading…</p>
    <p v-else-if="store.error" class="suppliers__error">{{ store.error }}</p>

    <div v-else class="suppliers__list">
      <div v-for="supplier in store.suppliers" :key="supplier.id" class="supplier-row">
        <span class="supplier-row__code">{{ supplier.code }}</span>
        <span class="supplier-row__name">{{ supplier.name }}</span>
        <span class="supplier-row__terms">{{ supplier.paymentTerms }}</span>
        <span
          class="supplier-row__status"
          :class="`supplier-row__status--${supplier.status.toLowerCase()}`"
        >
          {{ supplier.status }}
        </span>
      </div>
      <p v-if="store.suppliers.length === 0" class="suppliers__meta">No suppliers yet.</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.suppliers {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  flex: 1;
}

.suppliers__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.suppliers__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.suppliers__new-btn {
  background: var(--domain-procurement);
  color: var(--surface-1);
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
}

.suppliers__form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 10px;
  padding: 1rem;
  max-width: 360px;

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

.suppliers__meta {
  color: var(--text-muted);
  font-size: 14px;
}

.suppliers__error {
  color: var(--domain-danger);
  font-size: 13px;
}

.suppliers__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.supplier-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;
}

.supplier-row__code {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  color: var(--text-secondary);
  width: 90px;
  flex-shrink: 0;
}

.supplier-row__name {
  font-size: 14px;
  color: var(--text-primary);
  flex: 1;
}

.supplier-row__terms {
  font-size: 13px;
  color: var(--text-secondary);
}

.supplier-row__status {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.supplier-row__status--active {
  color: var(--domain-dispatch);
  background: color-mix(in srgb, var(--domain-dispatch) 12%, transparent);
}

.supplier-row__status--inactive {
  color: var(--text-muted);
  background: color-mix(in srgb, var(--text-muted) 12%, transparent);
}

.supplier-row__status--blacklisted {
  color: var(--domain-danger);
  background: color-mix(in srgb, var(--domain-danger) 12%, transparent);
}
</style>