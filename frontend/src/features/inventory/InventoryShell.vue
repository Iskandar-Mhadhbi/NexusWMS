<!--
  InventoryShell.vue
  Inventory pillar wrapper — tab-style sub-navigation, same pattern as
  ProcurementShell.vue. Stock and Zones tabs are placeholders until built.
-->
<script setup lang="ts">
</script>

<template>
  <div class="inventory-shell">
    <nav class="inventory-shell__tabs">
      <RouterLink :to="{ name: 'inventory-skus' }" class="inventory-shell__tab" active-class="inventory-shell__tab--active">
        SKUs
      </RouterLink>
      <RouterLink :to="{ name: 'inventory-stock' }" class="inventory-shell__tab" active-class="inventory-shell__tab--active">
        Stock
      </RouterLink>
      <RouterLink :to="{ name: 'inventory-zones' }" class="inventory-shell__tab" active-class="inventory-shell__tab--active">
        Zones
      </RouterLink>
    </nav>

    <RouterView />
  </div>
</template>

<style scoped lang="scss">
.inventory-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.inventory-shell__tabs {
  display: flex;
  flex-direction: row;
  gap: 4px;
  padding: 1rem 1.5rem 0;
  border-bottom: 0.5px solid var(--border);
}

.inventory-shell__tab {
  padding: 8px 14px;
  font-size: 14px;
  color: var(--text-secondary);
  text-decoration: none;
  border-bottom: 2px solid transparent;
}

.inventory-shell__tab--active {
  color: var(--domain-inventory);
  border-bottom-color: var(--domain-inventory);
  font-weight: 500;
}
</style>