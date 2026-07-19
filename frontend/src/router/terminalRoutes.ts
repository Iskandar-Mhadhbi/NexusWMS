/**
 * terminalRoutes.ts
 * Worker terminal branch — picker/packer/receiver/dispatcher, wrapped by
 * TerminalShell. No pillar nav, always dark, per
 * phase7_frontend_design.md §7.
 */
import type { RouteRecordRaw } from 'vue-router';

export const WORKER_ROLES = ['PICKER', 'PACKER', 'RECEIVER', 'DISPATCHER'];

export const terminalRoutes: RouteRecordRaw = {
  path: '/terminal',
  component: () => import('@/layout/terminal/TerminalShell.vue'),
  meta: { roles: WORKER_ROLES },
  children: [
    { path: 'pick', name: 'picker', component: () => import('@/features/picker/PickTerminalView.vue') },
    { path: 'pack', name: 'packer', component: () => import('@/features/packer/PackStationView.vue') },
    { path: 'receive', name: 'receiver', component: () => import('@/features/receiver/InboundReceivingView.vue') },
    { path: 'dispatch', name: 'dispatcher', component: () => import('@/features/dispatcher/DispatchDeskView.vue') },
  ],
};