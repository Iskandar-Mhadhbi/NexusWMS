<!--
  SkusView.vue
  Inventory pillar — SKU catalog list + create form, plus inline category
  creation. This is the real unblock for the "paste SKU UUID manually"
  workaround left in Purchase Orders / Goods Receipts — those forms are
  expected to be retrofitted onto skuStore separately, not in this pass.

  Dimensions (JSONB on the backend) are intentionally left out of the
  create form for now — sent as an empty object — to keep the form to a
  reasonable size. Add length/width/height inputs later if needed.
-->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useSkuStore } from '@/stores/skuStore';
import type { CreateSkuPayload, CreateCategoryPayload } from '@/core/models/sku';

const store = useSkuStore();

const search = ref('');
const showSkuForm = ref(false);
const showCategoryForm = ref(false);
const submitting = ref(false);
const submitError = ref('');

const skuForm = ref<CreateSkuPayload>({
  skuCode: '', name: '', categoryId: null, weightKg: 0, dimensions: {}, unit: 'unit', reorderPoint: 0, reorderQuantity: 0,
});
const categoryForm = ref<CreateCategoryPayload>({ name: '', parentId: null });

onMounted(() => store.load());

const filteredSkus = computed(() => {
  if (!search.value.trim()) return store.skus;
  const term = search.value.toLowerCase();
  return store.skus.filter((s) => s.skuCode.toLowerCase().includes(term) || s.name.toLowerCase().includes(term));
});

/** Submits the new-SKU form; resets and closes it on success. */
async function handleCreateSku() {
  submitting.value = true;
  submitError.value = '';
  try {
    await store.createSku(skuForm.value);
    skuForm.value = { skuCode: '', name: '', categoryId: null, weightKg: 0, dimensions: {}, unit: 'unit', reorderPoint: 0, reorderQuantity: 0 };
    showSkuForm.value = false;
  } catch (err) {
    submitError.value = 'Could not create SKU — check the fields and try again.';
    console.error('[SkusView] createSku failed:', err);
  } finally {
    submitting.value = false;
  }
}

/** Submits the new-category form; resets and closes it on success. */
async function handleCreateCategory() {
  submitting.value = true;
  submitError.value = '';
  try {
    await store.createCategory(categoryForm.value);
    categoryForm.value = { name: '', parentId: null };
    showCategoryForm.value = false;
  } catch (err) {
    submitError.value = 'Could not create category — check the fields and try again.';
    console.error('[SkusView] createCategory failed:', err);
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <div class="skus">
    <div class="skus__header">
      <h1 class="skus__title">SKUs</h1>
      <div class="skus__header-actions">
        <button class="skus__secondary-btn" @click="showCategoryForm = !showCategoryForm">
          {{ showCategoryForm ? 'Cancel' : 'New category' }}
        </button>
        <button class="skus__new-btn" @click="showSkuForm = !showSkuForm">
          {{ showSkuForm ? 'Cancel' : 'New SKU' }}
        </button>
      </div>
    </div>

    <form v-if="showCategoryForm" class="skus__form" @submit.prevent="handleCreateCategory">
      <input v-model="categoryForm.name" placeholder="Category name" required />
      <select v-model="categoryForm.parentId">
        <option :value="null">No parent (root category)</option>
        <option v-for="c in store.categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <button type="submit" :disabled="submitting">{{ submitting ? 'Saving…' : 'Save category' }}</button>
    </form>

    <form v-if="showSkuForm" class="skus__form" @submit.prevent="handleCreateSku">
      <input v-model="skuForm.skuCode" placeholder="SKU code (e.g. SKU-001)" required />
      <input v-model="skuForm.name" placeholder="Name" required />
      <select v-model="skuForm.categoryId">
        <option :value="null">No category</option>
        <option v-for="c in store.categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <input v-model.number="skuForm.weightKg" type="number" min="0" step="0.01" placeholder="Weight (kg)" />
      <input v-model="skuForm.unit" placeholder="Unit (e.g. each, box)" />
      <input v-model.number="skuForm.reorderPoint" type="number" min="0" placeholder="Reorder point" />
      <input v-model.number="skuForm.reorderQuantity" type="number" min="0" placeholder="Reorder quantity" />
      <button type="submit" :disabled="submitting">{{ submitting ? 'Saving…' : 'Save SKU' }}</button>
    </form>

    <p v-if="submitError" class="skus__error">{{ submitError }}</p>

    <input v-model="search" class="skus__search" placeholder="Search by code or name…" />

    <p v-if="store.loading" class="skus__meta">Loading…</p>
    <p v-else-if="store.error" class="skus__error">{{ store.error }}</p>

    <div v-else class="skus__list">
      <div v-for="sku in filteredSkus" :key="sku.id" class="sku-row">
        <span class="sku-row__code">{{ sku.skuCode }}</span>
        <span class="sku-row__name">{{ sku.name }}</span>
        <span class="sku-row__category">{{ store.categoryName(sku.categoryId) }}</span>
        <span class="sku-row__weight">{{ sku.weightKg }} kg</span>
        <span class="sku-row__reorder">Reorder @ {{ sku.reorderPoint }}</span>
      </div>
      <p v-if="filteredSkus.length === 0" class="skus__meta">No SKUs found.</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.skus {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  flex: 1;
}

.skus__header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.skus__header-actions {
  display: flex;
  flex-direction: row;
  gap: 8px;
}

.skus__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.skus__new-btn {
  background: var(--domain-inventory);
  color: var(--surface-1);
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
}

.skus__secondary-btn {
  background: none;
  border: 0.5px solid var(--border);
  color: var(--text-secondary);
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
}

.skus__form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 10px;
  padding: 1rem;
  max-width: 360px;

  input,
  select {
    padding: 8px 10px;
    border: 0.5px solid var(--border);
    border-radius: 6px;
    font-size: 14px;
    background: var(--surface-0);
    color: var(--text-primary);
  }

  button[type='submit'] {
    background: var(--domain-inventory);
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

.skus__search {
  padding: 10px 12px;
  border: 0.5px solid var(--border);
  border-radius: 8px;
  background: var(--surface-1);
  color: var(--text-primary);
  font-size: 14px;
  max-width: 320px;
}

.skus__meta {
  color: var(--text-muted);
  font-size: 14px;
}

.skus__error {
  color: var(--domain-danger);
  font-size: 13px;
}

.skus__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sku-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--surface-1);
  border: 0.5px solid var(--border);
  border-radius: 8px;
}

.sku-row__code {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 13px;
  color: var(--text-secondary);
  width: 100px;
  flex-shrink: 0;
}

.sku-row__name {
  font-size: 14px;
  color: var(--text-primary);
  flex: 1;
}

.sku-row__category {
  font-size: 12px;
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--domain-inventory) 12%, transparent);
  padding: 2px 8px;
  border-radius: 10px;
}

.sku-row__weight,
.sku-row__reorder {
  font-family: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}
</style>