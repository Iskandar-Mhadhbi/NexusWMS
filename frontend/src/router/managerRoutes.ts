/**
 * managerRoutes.ts
 * Manager shell branch — ADMIN/MANAGER/FINANCE/INVENTORY_CONTROLLER, per
 * phase7_frontend_design.md §7. Each child's meta.domain colors the left
 * accent bar in ManagerShell.vue. Procurement and Inventory delegate their
 * own children to procurementRoutes.ts / inventoryRoutes.ts rather than
 * nesting them inline here.
 */
import type { RouteRecordRaw } from 'vue-router';
import { procurementRoutes } from './procurementRoutes';
import { inventoryRoutes } from './inventoryRoutes';

export const MANAGER_ROLES = ['ADMIN', 'MANAGER', 'FINANCE', 'INVENTORY_CONTROLLER'];

export const managerRoutes: RouteRecordRaw = {
  path: '/',
  component: () => import('@/layout/manager/ManagerShell.vue'),
  meta: { roles: MANAGER_ROLES },
  children: [
    { path: '', name: 'live-ops', component: () => import('@/features/live-ops/LiveOpsView.vue'), meta: { domain: 'live-ops' } },
    {
      path: 'procurement',
      component: () => import('@/features/procurement/ProcurementShell.vue'),
      meta: { domain: 'procurement' },
      children: procurementRoutes,
    },
    {
      path: 'inventory',
      component: () => import('@/features/inventory/InventoryShell.vue'),
      meta: { domain: 'inventory' },
      children: inventoryRoutes,
    },
    { path: 'fulfillment', name: 'fulfillment', component: () => import('@/features/fulfillment/FulfillmentView.vue'), meta: { domain: 'fulfillment' } },
    { path: 'reports', name: 'reports', component: () => import('@/features/reports/ReportsView.vue'), meta: { domain: 'reports' } },
    { path: 'finance', name: 'finance', component: () => import('@/features/finance/FinanceView.vue'), meta: { domain: 'finance' } },
    { path: 'employees', name: 'employees', component: () => import('@/features/employees/EmployeesView.vue'), meta: { domain: 'employees' } },
  ],
};