/**
 * fulfillmentRoutes — routes for the Fulfillment pillar, mounted under
 * FulfillmentShell's tab nav. roles/meta mirrored from procurementRoutes.ts
 * pattern — verify against that file if this doesn't match exactly.
 */
import type { RouteRecordRaw } from 'vue-router';
import OrdersView from '@/features/fulfillment//OrdersView.vue';

export const fulfillmentRoutes: RouteRecordRaw[] = [
  { path: '', redirect: { name: 'fulfillment-orders' } },
  { path: 'orders', name: 'fulfillment-orders', component: OrdersView },
  { path: 'pick-lists', name: 'fulfillment-pick-lists', component: () => import('@/features/fulfillment/PickListsView.vue') },
  { path: 'packing', name: 'fulfillment-packing', component: () => import('@/features/fulfillment/PackingView.vue') },
  { path: 'dispatch', name: 'fulfillment-dispatch', component: () => import('@/features/fulfillment/DispatchView.vue') },
];