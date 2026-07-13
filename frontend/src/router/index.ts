/**
 * Application router entry point. Merges the three top-level branches —
 * auth, terminal, manager — each defined in its own file. Split out once
 * the flat route list grew unwieldy with nested pillar children.
 */
import { createRouter, createWebHistory } from 'vue-router';
import { authGuard } from '@/core/guards/authGuard';
import { roleGuard } from '@/core/guards/roleGuard';
import { authRoutes } from './authRoutes';
import { terminalRoutes } from './terminalRoutes';
import { managerRoutes } from './managerRoutes';

const router = createRouter({
  history: createWebHistory(),
  routes: [...authRoutes, terminalRoutes, managerRoutes],
});

router.beforeEach(authGuard);
router.beforeEach(roleGuard);

export default router;