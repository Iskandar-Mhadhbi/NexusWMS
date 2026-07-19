/**
 * procurementRoutes.ts
 * Children of the Procurement pillar tab shell (ProcurementShell.vue).
 * Split out on its own since sub-tabbed pillars are expected to keep
 * growing — Finance and Reports are the next candidates for the same
 * treatment once they need tabs.
 */
import type { RouteRecordRaw } from 'vue-router';

export const procurementRoutes: RouteRecordRaw[] = [
  { path: '', redirect: { name: 'procurement-suppliers' } },
  { path: 'suppliers', name: 'procurement-suppliers', component: () => import('@/features/procurement/suppliers/suppliersView.vue') },
  { path: 'purchase-orders', name: 'procurement-purchase-orders', component: () => import('@/features/procurement/purchase-orders/PurchaseOrdersView.vue') },
];