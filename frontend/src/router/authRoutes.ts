/**
 * authRoutes.ts
 * Public routes — no role guard applies before login.
 */
import type { RouteRecordRaw } from 'vue-router';

export const authRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/features/auth/login/LoginView.vue'),
  },
];