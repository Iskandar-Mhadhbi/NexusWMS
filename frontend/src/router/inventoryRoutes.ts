/**
 * inventoryRoutes.ts
 * Children of the Inventory pillar tab shell (InventoryShell.vue).
 */
import type { RouteRecordRaw } from 'vue-router';

export const inventoryRoutes: RouteRecordRaw[] = [
  { path: '', redirect: { name: 'inventory-skus' } },
  { path: 'skus', name: 'inventory-skus', component: () => import('@/features/inventory/SkuView.vue') },
  { path: 'stock', name: 'inventory-stock', component: () => import('@/features/inventory/StockView.vue') },
  { path: 'zones', name: 'inventory-zones', component: () => import('@/features/inventory/ZonesView.vue') },
];