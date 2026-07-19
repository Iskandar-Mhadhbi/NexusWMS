<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';

const identifier = ref('');
const password = ref('');
const error = ref('');
const auth = useAuthStore();
const router = useRouter();

async function handleLogin() {
  error.value = '';
  try {
    await auth.login(identifier.value, password.value);
    router.push('/'); // roleGuard bounces workers to their terminal automatically
  } catch {
    error.value = 'Invalid credentials';
  }
}
</script>

<template>
  <div class="login-page">
    <form class="login-form" @submit.prevent="handleLogin">
      <h1>NexusWMS</h1>
      <input v-model="identifier" placeholder="Email or Employee ID" />
      <input v-model="password" type="password" placeholder="Password" />
      <button type="submit">Log in</button>
      <p v-if="error">{{ error }}</p>
    </form>
  </div>
</template>

<style scoped lang="scss">
// Full-viewport centered login card. Background follows the active theme
// via --surface-0 rather than a hardcoded color, consistent with the rest of the manager-facing UI.
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--surface-0);
}
.login-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 320px;
}
</style>