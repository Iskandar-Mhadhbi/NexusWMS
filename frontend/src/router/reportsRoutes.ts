/**
 * Reports pillar child routes — nested under managerRoutes' 'reports' entry.
 * ReportsShell provides the tab nav (Daily report | Forecast).
 */
import type { RouteRecordRaw } from 'vue-router';

export const reportsRoutes: RouteRecordRaw[] = [
  { path: '', redirect: 'daily' },
  { path: 'daily', name: 'reports-daily', component: () => import('@/features/reports/DailyReportView.vue') },
  { path: 'forecast', name: 'reports-forecast', component: () => import('@/features/reports/ForecastView.vue') },
];