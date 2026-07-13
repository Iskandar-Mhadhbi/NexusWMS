<!--
  ProcurementShell.vue
  Procurement pillar wrapper — tab-style sub-navigation between Suppliers
  and Purchase Orders (Goods Receipts to follow later, per
  phase7_frontend_design.md §10). Supersedes the old flat "procurement"
  route that pointed straight at SuppliersView.
-->
<script setup lang="ts">
</script>

<template>
  <div class="procurement-shell">
    <nav class="procurement-shell__tabs">
      <RouterLink
        :to="{ name: 'procurement-suppliers' }"
        class="procurement-shell__tab"
        active-class="procurement-shell__tab--active"
      >
        Suppliers
      </RouterLink>
      <RouterLink
        :to="{ name: 'procurement-purchase-orders' }"
        class="procurement-shell__tab"
        active-class="procurement-shell__tab--active"
      >
        Purchase orders
      </RouterLink>
    </nav>

    <RouterView />
  </div>
</template>

<style scoped lang="scss">
.procurement-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.procurement-shell__tabs {
  display: flex;
  flex-direction: row;
  gap: 4px;
  padding: 1rem 1.5rem 0;
  border-bottom: 0.5px solid var(--border);
}

.procurement-shell__tab {
  padding: 8px 14px;
  font-size: 14px;
  color: var(--text-secondary);
  text-decoration: none;
  border-bottom: 2px solid transparent;
}

.procurement-shell__tab--active {
  color: var(--domain-procurement);
  border-bottom-color: var(--domain-procurement);
  font-weight: 500;
}
</style>