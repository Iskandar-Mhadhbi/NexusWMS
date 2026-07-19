/**
 * roleGuard.ts
 * Redirects an authenticated user away from any branch their role isn't
 * permitted for. Reads route.meta.roles — declared once on the top-level
 * branch routes (terminalRoutes.ts, managerRoutes.ts) — rather than
 * duplicating a role list here. Vue Router merges meta across every
 * matched route record for a navigation, so a leaf route like
 * inventory-skus inherits meta.roles from its ManagerShell-branch
 * ancestor automatically, with no per-child redeclaration needed.
 *
 * TERMINAL_ROUTE maps each worker role to its OWN specific terminal route
 * name. This stays separate from meta.roles on purpose: meta.roles answers
 * "is this role allowed here," but /terminal itself has no index redirect
 * (unlike / or /inventory), so a blocked worker needs somewhere concrete
 * to land, not just "into /terminal" generically.
 */
import type { NavigationGuardWithThis } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { WORKER_ROLES } from '@/router/terminalRoutes';

// Keys here must stay exactly WORKER_ROLES' values.
const TERMINAL_ROUTE: Record<string, string> = {
  PICKER: 'picker',
  PACKER: 'packer',
  RECEIVER: 'receiver',
  DISPATCHER: 'dispatcher',
};

export const roleGuard: NavigationGuardWithThis<undefined> = (to) => {
  const auth = useAuthStore();
  if (!auth.user) return;

  const allowedRoles = to.meta.roles;
  if (!allowedRoles || allowedRoles.includes(auth.user.role)) {
    return; // no restriction declared, or role is permitted — continue
  }

  // Wrong branch for this role — send them to their own home instead.
  const isWorker = WORKER_ROLES.includes(auth.user.role);
  return isWorker ? { name: TERMINAL_ROUTE[auth.user.role] } : { name: 'live-ops' };
};