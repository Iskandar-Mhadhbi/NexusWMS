<!--
  Manager shell: left-rail Sidebar + main content area. Opens the dashboard
  WebSocket once here (onMounted), not inside LiveOpsView — this is the app
  shell, so the connection survives navigation between manager pages
  (Procurement, Inventory, etc.) instead of dropping and reopening on every
  route change. See phase7_frontend_design.md §7.
-->
<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { useLiveOpsStore } from '@/stores/liveOpsStore';
import Sidebar from './Sidebar.vue';

const route = useRoute();
const liveOps = useLiveOpsStore();

/** CSS var reference for the current route's domain accent. */
const accentVar = computed(() => `var(--domain-${route.meta.domain ?? 'live-ops'})`);

onMounted(() => liveOps.connect());
onUnmounted(() => liveOps.disconnect());
</script>

<template>
  <div class="manager-shell">
    <Sidebar />
    <main class="manager-shell__content" :style="{ '--accent': accentVar }">
      <RouterView />
    </main>
  </div>
</template>

<style scoped lang="scss">
.manager-shell {
  display: flex;
  flex-direction: row;
  min-height: 100vh;
  background: var(--surface-0);
}

.manager-shell__content {
  display: flex;
  flex-direction: column;
  flex: 1;
  border-left: 3px solid var(--accent);
}
</style>