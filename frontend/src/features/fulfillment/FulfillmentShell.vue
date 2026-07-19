<!--
  FulfillmentShell.vue
  Fulfillment pillar tab wrapper — same pattern as ProcurementShell.vue /
  InventoryShell.vue. Orders is real; Pick Lists/Packing/Dispatch are
  placeholders until each sub-tab is built in a later pass.
-->
<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router';
</script>

<template>
  <div class="fulfillment-shell">
    <nav class="fulfillment-shell__tabs">
      <RouterLink to="/fulfillment/orders" class="fulfillment-shell__tab" active-class="fulfillment-shell__tab--active">
        Orders
      </RouterLink>
      <RouterLink to="/fulfillment/pick-lists" class="fulfillment-shell__tab" active-class="fulfillment-shell__tab--active">
        Pick Lists
      </RouterLink>
      <RouterLink to="/fulfillment/packing" class="fulfillment-shell__tab" active-class="fulfillment-shell__tab--active">
        Packing
      </RouterLink>
      <RouterLink to="/fulfillment/dispatch" class="fulfillment-shell__tab" active-class="fulfillment-shell__tab--active">
        Dispatch
      </RouterLink>
    </nav>
    <RouterView />
  </div>
</template>

<style scoped lang="scss">
.fulfillment-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.fulfillment-shell__tabs {
  display: flex;
  flex-direction: row;
  gap: 4px;
  padding: 0 1.5rem;
  border-bottom: 0.5px solid var(--border);
}

.fulfillment-shell__tab {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  border-bottom: 2px solid transparent;

  &--active {
    color: var(--domain-fulfillment);
    border-bottom-color: var(--domain-fulfillment);
  }
}
</style>