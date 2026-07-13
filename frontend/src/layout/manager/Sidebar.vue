<!--
  Left-rail navigation for the manager shell. Fixed width, always visible,
  two groups per phase7_frontend_design.md §6:
    - Operate: live warehouse floor pillars (color-coded by domain)
    - Manage:  back-office pillars (reports, finance, employees)
  Active route gets a tinted background, domain-colored text, and a dot.
-->
<script setup lang="ts">
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';

interface NavItem {
  routeName: string;
  label: string;
  domainVar: string;
}

const operateItems: NavItem[] = [
  { routeName: 'live-ops', label: 'Live ops', domainVar: '--domain-live-ops' },
  { routeName: 'procurement-suppliers', label: 'Procurement', domainVar: '--domain-procurement' },
  { routeName: 'inventory-skus', label: 'Inventory', domainVar: '--domain-inventory' },
  { routeName: 'fulfillment', label: 'Fulfillment', domainVar: '--domain-fulfillment' },
];

const manageItems: NavItem[] = [
  { routeName: 'reports', label: 'Reports', domainVar: '--domain-reports' },
  { routeName: 'finance', label: 'Finance', domainVar: '--domain-finance' },
  { routeName: 'employees', label: 'Employees', domainVar: '--domain-employees' },
];

const auth = useAuthStore();
const router = useRouter();

/** Logs the current user out and returns to the login screen. */
function handleLogout() {
  auth.logout();
  router.push({ name: 'login' });
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar__brand">NexusWMS</div>

    <nav class="sidebar__group">
      <span class="sidebar__group-label">Operate</span>
      <RouterLink
        v-for="item in operateItems"
        :key="item.routeName"
        :to="{ name: item.routeName }"
        class="sidebar__item"
        active-class="sidebar__item--active"
        :style="{ '--item-color': `var(${item.domainVar})` }"
      >
        <span class="sidebar__dot"></span>
        {{ item.label }}
      </RouterLink>
    </nav>

    <nav class="sidebar__group">
      <span class="sidebar__group-label">Manage</span>
      <RouterLink
        v-for="item in manageItems"
        :key="item.routeName"
        :to="{ name: item.routeName }"
        class="sidebar__item"
        active-class="sidebar__item--active"
        :style="{ '--item-color': `var(${item.domainVar})` }"
      >
        <span class="sidebar__dot"></span>
        {{ item.label }}
      </RouterLink>
    </nav>

    <div class="sidebar__footer">
      <div class="sidebar__user">
        <span class="sidebar__user-name">{{ auth.user?.name }}</span>
        <span class="sidebar__user-role">{{ auth.user?.role }}</span>
      </div>
      <button class="sidebar__logout" @click="handleLogout">Log out</button>
    </div>
  </aside>
</template>

<style scoped lang="scss">
.sidebar {
  display: flex;
  flex-direction: column;
  width: 220px;
  flex-shrink: 0;
  background: var(--surface-1);
  border-right: 0.5px solid var(--border);
  padding: 1rem 0.75rem;
  gap: 1.5rem;
}

.sidebar__brand {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
  padding: 0 0.5rem;
}

.sidebar__group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar__group-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 0 0.5rem;
  margin-bottom: 4px;
}

.sidebar__item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  text-decoration: none;

  &:hover {
    background: var(--surface-0);
  }
}

.sidebar__item--active {
  background: var(--surface-0);
  color: var(--item-color);
  font-weight: 500;
}

.sidebar__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--item-color);
  flex-shrink: 0;
}

.sidebar__footer {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 1rem;
  border-top: 0.5px solid var(--border);
}

.sidebar__user {
  display: flex;
  flex-direction: column;
  padding: 0 0.5rem;
}

.sidebar__user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.sidebar__user-role {
  font-family: "JetBrains Mono", "SF Mono", Consolas, monospace;
  font-size: 11px;
  color: var(--text-muted);
}

.sidebar__logout {
  align-self: flex-start;
  margin-left: 0.5rem;
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 0;

  &:hover {
    color: var(--domain-danger);
  }
}
</style>