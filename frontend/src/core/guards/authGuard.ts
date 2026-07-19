import type { NavigationGuardWithThis } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';

export const authGuard: NavigationGuardWithThis<undefined> = (to) => {
  const auth = useAuthStore();
  if (!auth.isAuthenticated && to.name !== 'login') {
    return { name: 'login' };
  }
};