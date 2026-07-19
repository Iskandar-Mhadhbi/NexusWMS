<script setup lang="ts">
</script>

<template>
  <div class="terminal-shell" data-theme="dark">
    <RouterView />
  </div>
</template>

<style scoped lang="scss">
.terminal-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #1c1c1a;
}
</style>