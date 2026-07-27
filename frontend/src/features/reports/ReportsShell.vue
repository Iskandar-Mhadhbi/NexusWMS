<!--
  Reports pillar shell — tab-style sub-nav (Daily report | Forecast),
  same pattern as ProcurementShell/InventoryShell/FulfillmentShell.
-->
<script setup lang="ts"></script>

<template>
  <div class="reports-shell">
    <nav class="reports-shell__tabs flex-row">
      <RouterLink to="/reports/daily" class="reports-shell__tab">Daily report</RouterLink>
      <RouterLink to="/reports/forecast" class="reports-shell__tab">Forecast</RouterLink>
    </nav>
    <div class="reports-shell__content">
      <RouterView />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.flex-row { display: flex; align-items: center; gap: 0.5rem; }

.reports-shell {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;

  &__tabs {
    justify-content: flex-start;
    border-bottom: 0.5px solid var(--border);
  }

  &__tab {
    padding: 0.6rem 1rem;
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--text-secondary);
    text-decoration: none;
    border-bottom: 2px solid transparent;

    &.router-link-active {
      color: var(--domain-reports, #2C8C89);
      border-bottom-color: var(--domain-reports, #2C8C89);
    }
  }
}
</style>